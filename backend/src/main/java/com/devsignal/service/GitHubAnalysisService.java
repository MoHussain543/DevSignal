package com.devsignal.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

import com.devsignal.client.github.GitHubClient;
import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.dto.analysis.ReadmeAnalysisDto;
import com.devsignal.dto.analysis.RepoAnalysisDto;
import com.devsignal.dto.analysis.ScoreBreakdownDto;
import com.devsignal.dto.analysis.WeightedScoreBreakdownDto;
import com.devsignal.dto.github.GitHubProfileDto;
import com.devsignal.dto.github.GitHubRepoDto;

@Service
public class GitHubAnalysisService {

	private static final int MAX_DEEP_ANALYSIS_REPOS = 10;
	private static final int MIN_PORTFOLIO_REPO_COUNT = 3;
	private static final int MIN_DISTINCT_LANGUAGES_PORTFOLIO = 3;
	private static final int STRONG_DESCRIPTION_MIN_LENGTH = 25;

	/**
	 * Weight on average readiness so that {@code avg=100} plus max repo-count bonus ({@link #portfolioRepoCountBonus})
	 * reaches exactly 100. Keeps weighted profile points aligned with {@code …/100} labeling.
	 */
	private static final float PROJECT_QUALITY_AVG_WEIGHT = 0.70f;

	/**
	 * Portion of the README rubric blended into composite repo scores (README still has its own pillar).
	 * Together with {@code +45} max hygiene metadata and {@code TOP_STAR_BUCKET_BONUS} (+30 at 10k+ stars),
	 * this keeps a perfect readme rubric {@code × 25 / 100} able to composite to exactly {@code 100}.
	 */
	private static final int README_SCORE_PERCENT_OF_REPO_TOTAL = 25;

	/** Largest star-tier bonus before {@code clampRepoScore(metadata)} caps metadata at 100. */
	private static final int TOP_STAR_BUCKET_BONUS = 30;


	private static final int RW_ORIGINAL_WEIGHT = 15;
	private static final int RW_LANGUAGE_WEIGHT = 10;
	private static final int RW_STRONG_DESCRIPTION_WEIGHT = 10;
	private static final int RW_RECENCY_WEIGHT = 10;

	private static final List<String> DEMO_DESCRIPTION_PHRASES =
			List.of("for demonstration purposes only", "my first repository", "sample repository");
	private static final Pattern DEMO_NAME_PATTERN = Pattern.compile(
			"^(hello[-_ ]world|spoon[-_ ]knife|test(?:[-_ ]repo)?\\d*|demo(?:[-_ ].*)?|sample(?:[-_ ].*)?)$",
			Pattern.CASE_INSENSITIVE);

	private static final List<String> TECH_STACK_MARKERS = List.of(
			"tech stack", "technologies", "built with",
			"java", "spring boot", "react", "postgresql", "mongodb", "docker");

	private final GitHubClient gitHubClient;

	public GitHubAnalysisService(GitHubClient gitHubClient) {
		this.gitHubClient = gitHubClient;
	}

	public AnalysisResponseDto analyze(String username) {
		String normalizedUsername = username == null ? "" : username.strip();
		if (normalizedUsername.isEmpty()) {
			throw new IllegalArgumentException("GitHub username must not be blank");
		}

		GitHubProfileDto profile = gitHubClient.getUserProfile(normalizedUsername);
		List<GitHubRepoDto> repos = safeRepos(gitHubClient.getUserRepos(normalizedUsername));

		int totalFetchedRepoCount = repos.size();
		int originalRepoCount = (int) repos.stream().filter(Objects::nonNull).filter(r -> !r.fork()).count();
		int forkedRepoCount = totalFetchedRepoCount - originalRepoCount;
		boolean allReposAreForks = !repos.isEmpty() && originalRepoCount == 0;

		String ownerLogin = nullToEmpty(profile.login());
		if (ownerLogin.isEmpty()) {
			ownerLogin = normalizedUsername;
		}

		Instant sixMonthsAgo = sixMonthsAgoUtcInstant();
		List<GitHubRepoDto> deepAnalysisRepos = selectTopReposForDeepAnalysis(repos, sixMonthsAgo);
		List<RepoAnalysisDto> repoAnalyses = buildRepoAnalyses(deepAnalysisRepos, sixMonthsAgo, ownerLogin);

		List<RepoAnalysisDto> portfolioRepos =
				repoAnalyses.stream().filter(ra -> ra.originalRepo() && !ra.likelyDemoRepo()).toList();

		int analyzedRepoCount = repoAnalyses.size();
		int ignoredRepoCount = totalFetchedRepoCount - analyzedRepoCount;
		// Original repos within the analyzed sample — same population as portfolioRepoCount.
		// Used as denominator for originality scoring to avoid mixing sampled vs full-population counts.
		int analyzedOriginalRepoCount = (int) repoAnalyses.stream().filter(r -> !r.fork()).count();
		int portfolioRepoCount = portfolioRepos.size();

		int portfolioAverageRepoScore = portfolioRepos.isEmpty()
				? 0
				: (int) Math.round(
						portfolioRepos.stream().mapToInt(RepoAnalysisDto::repoScore).average().orElse(0.0));

		int portfolioAverageMetadataRepoScore = portfolioRepos.isEmpty()
				? 0
				: (int) Math.round(
						portfolioRepos.stream().mapToInt(RepoAnalysisDto::repoMetadataScore).average().orElse(0.0));

		long portfolioTotalStars = portfolioRepos.stream().mapToLong(RepoAnalysisDto::stars).sum();
		long portfolioTotalForks = portfolioRepos.stream().mapToLong(RepoAnalysisDto::forks).sum();

		Map<String, Long> languageCounts = portfolioLanguageCounts(portfolioRepos);
		List<String> portfolioTopLanguages = topLanguagesDescending(languageCounts);
		long distinctLangCount = distinctLanguageCount(languageCounts);

		boolean repoCountStrength = portfolioRepoCount >= MIN_PORTFOLIO_REPO_COUNT;
		boolean starsStrength = portfolioTotalStars > 0;
		boolean languagesStrength = distinctLanguageCount(languageCounts) >= MIN_DISTINCT_LANGUAGES_PORTFOLIO;
		boolean descriptionStrength = majorityPortfolioMatch(portfolioRepos, RepoAnalysisDto::hasStrongDescription);
		boolean recentActivityStrength =
				majorityPortfolioMatch(portfolioRepos, RepoAnalysisDto::recentlyUpdated);

		ScoreBreakdownDto scoreBreakdown = buildScoreBreakdown(
				portfolioRepoCount,
				portfolioAverageMetadataRepoScore,
				distinctLangCount,
				portfolioRepos,
				analyzedOriginalRepoCount);

		WeightedScoreBreakdownDto weightedScoreBreakdown = buildWeightedScoreBreakdown(scoreBreakdown);
		int score = clampBreakdown(weightedScoreBreakdown.totalPoints());

		List<String> strengths = new ArrayList<>();
		List<String> weaknesses = new ArrayList<>();

		if (allReposAreForks) {
			weaknesses.add(
					"Every repo we reviewed looks like a fork, so there is little original portfolio work to score yet.");
		}

		if (portfolioRepos.isEmpty()) {
			weaknesses.add("We did not find portfolio-style repos in what we reviewed (original work that is not mostly a demo or fork).");
		}
		else {
			if (repoCountStrength) {
				strengths.add("You have several portfolio repos in the set we reviewed.");
			}
			else {
				weaknesses.add("Fewer than three portfolio repos showed up—adding more original projects would help.");
			}
			if (starsStrength) {
				strengths.add("At least one reviewed repo has stars.");
			}
			else {
				weaknesses.add("Your portfolio repos do not have stars yet—that is normal early on.");
			}
			if (languagesStrength) {
				strengths.add("Your portfolio shows work across several languages.");
			}
			else {
				weaknesses.add("Most of the reviewed work is concentrated in one or two languages—more variety would help.");
			}
			if (descriptionStrength) {
				strengths.add("Some of your strongest repos have clear descriptions.");
			}
			else {
				weaknesses.add("Several repos need clearer descriptions.");
			}
			if (recentActivityStrength) {
				strengths.add("At least one portfolio repo was updated recently.");
			}
			else {
				weaknesses.add("No portfolio repos were updated in the last six months—consider a small refresh.");
			}
		}

		appendBreakdownStrengths(strengths, scoreBreakdown);

		String candidateLevel = resolveCandidateLevel(score);
		String hiringRecommendation = resolveHiringRecommendation(score);
		List<String> technicalHighlights =
				buildTechnicalHighlights(repoCountStrength, recentActivityStrength, starsStrength,
						languagesStrength, descriptionStrength);
		List<String> growthAreas = buildGrowthAreas(
				portfolioRepos.isEmpty(),
				allReposAreForks,
				repoCountStrength,
				descriptionStrength,
				languagesStrength,
				recentActivityStrength,
				scoreBreakdown);

		long readmeWeakCount =
				portfolioRepos.stream().filter(r -> r.readmeAnalysis().documentationScore() < 40).count();
		long readmeMissing =
				portfolioRepos.stream().filter(r -> !r.readmeAnalysis().hasReadme()).count();
		int forkRepoCount =
				(int) repoAnalyses.stream().filter(RepoAnalysisDto::fork).count();
		int demoRepoCount =
				(int) repoAnalyses.stream().filter(RepoAnalysisDto::likelyDemoRepo).count();

		String scoreExplanation =
				buildScoreExplanation(
						score,
						scoreBreakdown,
						weightedScoreBreakdown,
						portfolioRepos.isEmpty(),
						allReposAreForks,
						portfolioRepoCount,
						recentActivityStrength,
						descriptionStrength,
						weaknesses,
						growthAreas);
		String projectQualityExplanation =
				buildProjectQualityExplanation(portfolioRepoCount, portfolioAverageMetadataRepoScore, forkRepoCount);
		String technicalBreadthExplanation =
				buildTechnicalBreadthExplanation(distinctLangCount, portfolioTopLanguages, portfolioRepos.isEmpty());
		String documentationExplanation =
				buildDocumentationExplanation(
						portfolioRepoCount,
						scoreBreakdown.documentationScore(),
						readmeWeakCount,
						readmeMissing);
		String originalityExplanation =
				buildOriginalityExplanation(originalRepoCount, analyzedOriginalRepoCount, portfolioRepoCount, forkRepoCount, demoRepoCount);
		String activityExplanation =
				buildActivityExplanation(
						portfolioRepoCount,
						scoreBreakdown.activityScore(),
						recentActivityStrength);

		String resolvedLogin = nullToEmpty(profile.login());
		String displayUsername = resolvedLogin.isEmpty() ? normalizedUsername : resolvedLogin;

		List<RepoAnalysisDto> sortedRepos = sortReposForDisplay(repoAnalyses);
		FeaturedSelection featured = selectFeaturedRepo(portfolioRepos);

		return new AnalysisResponseDto(
				displayUsername,
				profile.name(),
				profile.avatarUrl(),
				profile.bio(),
				profile.publicRepos(),
				profile.followers(),
				analyzedRepoCount,
				totalFetchedRepoCount,
				ignoredRepoCount,
				originalRepoCount,
				forkedRepoCount,
				portfolioRepoCount,
				portfolioTotalStars,
				portfolioTotalForks,
				portfolioAverageRepoScore,
				portfolioTopLanguages,
				candidateLevel,
				hiringRecommendation,
				scoreExplanation,
				projectQualityExplanation,
				technicalBreadthExplanation,
				documentationExplanation,
				originalityExplanation,
				activityExplanation,
				scoreBreakdown,
				weightedScoreBreakdown,
				List.copyOf(technicalHighlights),
				List.copyOf(growthAreas),
				List.copyOf(sortedRepos),
				featured.repo(),
				featured.reason(),
				List.copyOf(strengths),
				List.copyOf(weaknesses),
				score
		);
	}

	private static ScoreBreakdownDto buildScoreBreakdown(
			int portfolioRepoCount,
			int portfolioAverageMetadataRepoScore,
			long distinctLangCount,
			List<RepoAnalysisDto> portfolioRepos,
			int analyzedOriginalRepoCount) {

		int projectQualityScore =
				portfolioRepoCount == 0 && portfolioAverageMetadataRepoScore == 0
						? 0
						: clampBreakdown(
								Math.round(portfolioAverageMetadataRepoScore * PROJECT_QUALITY_AVG_WEIGHT
										+ portfolioRepoCountBonus(portfolioRepoCount)));

		int technicalBreadthScore =
				distinctLangCount == 0
						? 0
						: clampBreakdown((int) Math.round(
								100.0 * Math.log(distinctLangCount + 1) / Math.log(9.0)));

		long recentCount = portfolioRepos.stream().filter(RepoAnalysisDto::recentlyUpdated).count();
		int activityScore = portfolioRepoCount == 0
				? 0
				: clampBreakdown((int) (recentCount * 100 / portfolioRepoCount));

		long readmeDocSum = portfolioRepos.stream().mapToLong(r -> r.readmeAnalysis().documentationScore()).sum();
		int documentationScore = portfolioRepoCount == 0
				? 0
				: clampBreakdown((int) Math.round(readmeDocSum / (double) portfolioRepoCount));

		int originalityScore = analyzedOriginalRepoCount == 0
				? 0
				: clampBreakdown((int) Math.round(
						portfolioRepoCount * 75.0 / analyzedOriginalRepoCount + Math.min(25, portfolioRepoCount * 8L)));

		return new ScoreBreakdownDto(
				projectQualityScore,
				technicalBreadthScore,
				activityScore,
				documentationScore,
				originalityScore);
	}

	private static WeightedScoreBreakdownDto buildWeightedScoreBreakdown(ScoreBreakdownDto breakdown) {
		int projectQualityPoints = clampWeightedPoints(
				Math.round(breakdown.projectQualityScore() * 30.0 / 100.0), 30);
		int technicalBreadthPoints = clampWeightedPoints(
				Math.round(breakdown.technicalBreadthScore() * 20.0 / 100.0), 20);
		int documentationPoints = clampWeightedPoints(
				Math.round(breakdown.documentationScore() * 20.0 / 100.0), 20);
		int originalityPoints = clampWeightedPoints(
				Math.round(breakdown.originalityScore() * 20.0 / 100.0), 20);
		int activityPoints = clampWeightedPoints(
				Math.round(breakdown.activityScore() * 10.0 / 100.0), 10);
		int totalPoints = clampBreakdown(projectQualityPoints + technicalBreadthPoints + documentationPoints
				+ originalityPoints + activityPoints);
		return new WeightedScoreBreakdownDto(
				projectQualityPoints,
				technicalBreadthPoints,
				documentationPoints,
				originalityPoints,
				activityPoints,
				totalPoints);
	}

	private static int clampWeightedPoints(long value, int max) {
		return (int) Math.min(max, Math.max(0, value));
	}

	private static int clampBreakdown(int value) {
		return Math.min(100, Math.max(0, value));
	}

	/** True when strictly more than half of qualifying repos match (avoids {@code anyMatch} overstatement). */
	private static boolean majorityPortfolioMatch(List<RepoAnalysisDto> portfolioRepos, Predicate<RepoAnalysisDto> test) {
		if (portfolioRepos.isEmpty()) {
			return false;
		}
		long hits = portfolioRepos.stream().filter(test).count();
		return hits * 2 > portfolioRepos.size();
	}

	/** Smoothed bonus for qualifying portfolio repo count (less jumpy than a linear per-repo ramp). */
	private static int portfolioRepoCountBonus(int portfolioRepoCount) {
		if (portfolioRepoCount <= 0) {
			return 0;
		}
		if (portfolioRepoCount == 1) {
			return 10;
		}
		if (portfolioRepoCount == 2) {
			return 18;
		}
		if (portfolioRepoCount == 3) {
			return 25;
		}
		return 30;
	}

	private record FeaturedSelection(RepoAnalysisDto repo, String reason) {
	}

	private static FeaturedSelection selectFeaturedRepo(List<RepoAnalysisDto> portfolioRepos) {
		if (portfolioRepos == null || portfolioRepos.isEmpty()) {
			return new FeaturedSelection(null, null);
		}
		RepoAnalysisDto pick = portfolioRepos.stream().max(FEATURED_REPO_COMPARATOR).orElse(null);
		if (pick == null) {
			return new FeaturedSelection(null, null);
		}
		String reason =
				"Highest overall quality among your original, non-demo portfolio repos. "
						+ "If scores tie, we favor stronger READMEs, more recent updates, then star count.";
		return new FeaturedSelection(pick, reason);
	}

	private static final Comparator<RepoAnalysisDto> FEATURED_REPO_COMPARATOR =
			Comparator.comparingInt(RepoAnalysisDto::repoScore)
					.thenComparingInt(r -> r.readmeAnalysis().documentationScore())
					.thenComparing(RepoAnalysisDto::recentlyUpdated)
					.thenComparingInt(RepoAnalysisDto::stars)
					.thenComparing(r -> r.name() == null ? "" : r.name(), String.CASE_INSENSITIVE_ORDER);

	private static String resolveCandidateLevel(int score) {
		if (score >= 85) {
			return "Strong portfolio signal";
		}
		if (score >= 70) {
			return "Promising portfolio signal";
		}
		if (score >= 50) {
			return "Developing portfolio";
		}
		return "Early-stage portfolio";
	}

	private static String resolveHiringRecommendation(int score) {
		if (score >= 85) {
			return "This reads like a strong GitHub portfolio overall.";
		}
		if (score >= 70) {
			return "Solid portfolio with a few areas to polish.";
		}
		if (score >= 50) {
			return "Some good building blocks—depth and documentation can go further.";
		}
		return "Early-stage portfolio—keep shipping original work.";
	}

	private static List<String> buildTechnicalHighlights(
			boolean repoCountStrength,
			boolean recentActivityStrength,
			boolean starsStrength,
			boolean languagesStrength,
			boolean descriptionStrength) {
		List<String> highlights = new ArrayList<>();
		if (repoCountStrength) {
			highlights.add("You have multiple portfolio repos in the set we reviewed.");
		}
		if (recentActivityStrength) {
			highlights.add("Most portfolio repos were updated recently.");
		}
		if (starsStrength) {
			highlights.add("At least one reviewed repo has stars.");
		}
		if (languagesStrength) {
			highlights.add("Your portfolio shows work across multiple technologies.");
		}
		if (descriptionStrength) {
			highlights.add("Some of your strongest repos have clear descriptions.");
		}
		return highlights;
	}

	private static List<String> buildGrowthAreas(
			boolean portfolioEmpty,
			boolean allReposAreForks,
			boolean repoCountStrength,
			boolean descriptionStrength,
			boolean languagesStrength,
			boolean recentActivityStrength,
			ScoreBreakdownDto scoreBreakdown) {
		List<String> areas = new ArrayList<>();
		if (portfolioEmpty || allReposAreForks) {
			areas.add("Build more original, non-demo projects");
		}
		else if (!repoCountStrength) {
			areas.add("Build more original, non-demo projects");
		}
		if (!portfolioEmpty && !descriptionStrength) {
			areas.add("Add clearer descriptions to more repos");
			areas.add("Add stronger README files with setup and usage instructions");
		}
		else if (portfolioEmpty) {
			areas.add("Add clearer descriptions to more repos");
			areas.add("Add stronger README files with setup and usage instructions");
		}
		if (!portfolioEmpty && !languagesStrength) {
			areas.add("Show more variety in technologies or project types");
		}
		else if (portfolioEmpty) {
			areas.add("Show more variety in technologies or project types");
		}
		if (!portfolioEmpty && !recentActivityStrength) {
			areas.add("Keep more portfolio repos active");
		}
		else if (portfolioEmpty) {
			areas.add("Keep more portfolio repos active");
		}
		appendBreakdownGrowthAreas(areas, scoreBreakdown);
		return areas;
	}

	private static void appendBreakdownStrengths(List<String> strengths, ScoreBreakdownDto breakdown) {
		if (breakdown.documentationScore() >= 70) {
			addOnce(strengths, "README and documentation look strong across your portfolio repos.");
		}
		if (breakdown.originalityScore() >= 70) {
			addOnce(strengths, "You have original repos that count as real portfolio work.");
		}
		if (breakdown.technicalBreadthScore() >= 70) {
			addOnce(strengths, "Your portfolio shows breadth across languages or stacks.");
		}
	}

	private static void appendBreakdownGrowthAreas(List<String> areas, ScoreBreakdownDto breakdown) {
		if (breakdown.documentationScore() < 40) {
			addOnce(areas, "README files need more setup or usage details");
		}
		if (breakdown.originalityScore() < 40) {
			addOnce(areas, "Build more original, non-demo projects");
		}
		if (breakdown.technicalBreadthScore() < 40) {
			addOnce(areas, "Show more variety in technologies or stacks across your repos");
		}
	}

	private static void addOnce(List<String> list, String message) {
		if (!list.contains(message)) {
			list.add(message);
		}
	}

	private static List<RepoAnalysisDto> sortReposForDisplay(List<RepoAnalysisDto> repos) {
		return repos.stream()
				.sorted(REPO_DISPLAY_COMPARATOR)
				.toList();
	}

	private static final Comparator<RepoAnalysisDto> REPO_DISPLAY_COMPARATOR =
			Comparator.<RepoAnalysisDto>comparingInt(GitHubAnalysisService::repoDisplayTier)
					.thenComparing(Comparator.<RepoAnalysisDto>comparingInt(RepoAnalysisDto::repoScore).reversed())
					.thenComparing(
							(RepoAnalysisDto r) -> r.name() == null ? "" : r.name(),
							String.CASE_INSENSITIVE_ORDER);

	/**
	 * 0 — original non-demo repos (qualifying portfolio), 1 — original demos/samples, 2 — forks lowest.
	 */
	private static int repoDisplayTier(RepoAnalysisDto r) {
		if (!r.fork() && !r.likelyDemoRepo()) {
			return 0;
		}
		if (!r.fork()) {
			return 1;
		}
		return 2;
	}

	private static String buildScoreExplanation(
			int score,
			ScoreBreakdownDto b,
			WeightedScoreBreakdownDto weighted,
			boolean portfolioEmpty,
			boolean allReposAreForks,
			int portfolioRepoCount,
			boolean recentActivityStrength,
			boolean descriptionStrength,
			List<String> weaknesses,
			List<String> growthAreas) {
		StringBuilder sb = new StringBuilder().append("This profile scores ").append(score).append("/100");
		if (portfolioEmpty) {
			if (allReposAreForks) {
				sb.append(
						" because every repo we reviewed looks like a fork, ")
						.append("so we could not anchor a portfolio score on original repos");
			}
			else {
				sb.append(
						" because we did not find original, portfolio-style repos in what we reviewed (often demos or forks sit in that gap)");
			}
			return sentenceDot(sb.toString());
		}

		ArrayList<String> positives = new ArrayList<>();
		if (recentActivityStrength) {
			positives.add("recent pushes show up across your portfolio repos");
		}
		if (weighted.projectQualityPoints() >= 20 && portfolioRepoCount >= MIN_PORTFOLIO_REPO_COUNT) {
			positives.add("your strongest repos look polished on the metrics we checked");
		}
		else if (portfolioAveragePassesThreshold(portfolioRepoCount, weighted.projectQualityPoints())) {
			positives.add("your best repos lift the averages for the repos we sampled");
		}
		if (b.documentationScore() >= 60) {
			positives.add("README quality looks healthy on average across portfolio repos");
		}
		else if (descriptionStrength) {
			positives.add("several repos have solid short descriptions even when README depth varies");
		}
		if (b.technicalBreadthScore() >= 68 && portfolioRepoCount > 0) {
			positives.add("your portfolio shows work across multiple languages");
		}
		if (b.originalityScore() >= 65) {
			positives.add("most of the reviewed originals look like shipped work—not just forks");
		}

		ArrayList<String> negatives = scoreGapPhrases(b, portfolioRepoCount, recentActivityStrength, descriptionStrength);

		if (!weaknesses.isEmpty() && negatives.size() < 3) {
			String w = softenWeaknessCue(weaknesses.getFirst());
			if (!w.isBlank() && negatives.stream().noneMatch(w::equals)) {
				negatives.add(w);
			}
		}
		if (!growthAreas.isEmpty() && negatives.size() < 4) {
			String growthFirst = growthAreas.getFirst();
			String gLower = lowerFirstLetter(growthFirst);
			String cue = growthAreaCue(growthFirst);
			if (!cue.isBlank() && negatives.stream().noneMatch(v -> v.equals(cue))) {
				negatives.addFirst(cue);
			}
			else if (cue.isBlank() && !gLower.isBlank()) {
				negatives.addFirst("priorities skew toward ".concat(gLower));
			}
		}

		positiveTrim(positives);

		sb.append(" because ");
		if (!positives.isEmpty() && !negatives.isEmpty()) {
			List<String> positiveSlice = positives.subList(0, Math.min(2, positives.size()));
			List<String> negativeSlice = negatives.subList(0, Math.min(3, negatives.size()));
			sb.append(joinBulletsNatural(positiveSlice))
					.append(", yet ")
					.append(joinBulletsNatural(negativeSlice));
		}
		else if (!negatives.isEmpty()) {
			sb.append(joinBulletsNatural(negatives.subList(0, Math.min(4, negatives.size()))));
		}
		else if (!positives.isEmpty()) {
			sb.append(joinBulletsNatural(positives))
					.append(" with no big structural gaps jumping out among the repos we reviewed");
		}
		else {
			sb.append("the picture is fairly even across repos we reviewed ")
					.append("without one category clearly running ahead of the others");
		}

		return sentenceDot(sb.toString());
	}

	private static boolean portfolioAveragePassesThreshold(int portfolioRepoCount, int projectQualityWeighted) {
		return portfolioRepoCount > 0 && projectQualityWeighted >= 15;
	}

	private static void positiveTrim(ArrayList<String> positives) {
		while (positives.size() > 2) {
			positives.removeLast();
		}
	}

	private static ArrayList<String> scoreGapPhrases(
			ScoreBreakdownDto b,
			int portfolioRepoCount,
			boolean recentActivityStrength,
			boolean descriptionStrength) {
		ArrayList<String> phrases = new ArrayList<>();
		if (portfolioRepoCount < MIN_PORTFOLIO_REPO_COUNT) {
			if (portfolioRepoCount == 1) {
				addPhraseIfNew(phrases, "only one portfolio repo appeared in our review");
			}
			else if (portfolioRepoCount == 0) {
				addPhraseIfNew(phrases, "no portfolio repos showed up in what we reviewed");
			}
			else {
				addPhraseIfNew(phrases, "fewer than three portfolio repos were in the sample we opened");
			}
		}
		if (b.projectQualityScore() < 48 && portfolioRepoCount > 0) {
			addPhraseIfNew(
					phrases,
					"overall polish on metadata and completeness trails stronger profiles overall");
		}
		if (b.technicalBreadthScore() <= 44) {
			if (portfolioRepoCount <= 1) {
				addPhraseIfNew(
						phrases,
						"most of the reviewed work is concentrated in one language footprint");
			}
			else {
				addPhraseIfNew(
						phrases,
						"language variety stays narrow versus profiles that show more stacks");
			}
		}
		if (b.documentationScore() <= 42) {
			addPhraseIfNew(
					phrases,
					"README files need more setup or usage detail on average across portfolio repos");
		}
		else if (!descriptionStrength && b.documentationScore() < 62) {
			addPhraseIfNew(
					phrases,
					"few repos pair crisp descriptions with deep README sections");
		}
		if (b.originalityScore() <= 40) {
			addPhraseIfNew(
					phrases,
					"many repos look like demos, forks, or sample projects—which pulls the originality reading down");
		}
		if (b.activityScore() <= 42 && !recentActivityStrength && portfolioRepoCount > 0) {
			addPhraseIfNew(
					phrases,
					"most portfolio repos have been quiet over the past six months");
		}
		return phrases;
	}

	private static void addPhraseIfNew(ArrayList<String> list, String phrase) {
		if (!phrase.isBlank() && !list.contains(phrase)) {
			list.add(phrase);
		}
	}

	private static String sentenceDot(String raw) {
		String t = sanitizeSpaces(raw).replaceAll("(\\.+)$", "").stripTrailing();
		return (t.endsWith(",") ? t.substring(0, t.length() - 1).stripTrailing() : t).concat(".");
	}

	private static String sanitizeSpaces(String raw) {
		return raw == null ? "" : raw.replaceAll("\\s+", " ").strip();
	}

	private static String joinBulletsNatural(List<String> phrases) {
		if (phrases.isEmpty()) {
			return "";
		}
		if (phrases.size() == 1) {
			return phrases.getFirst();
		}
		if (phrases.size() == 2) {
			return phrases.get(0).concat(" and ").concat(phrases.get(1));
		}
		StringBuilder sb = new StringBuilder(phrases.getFirst());
		for (int i = 1; i < phrases.size() - 1; i++) {
			sb.append(", ").append(phrases.get(i));
		}
		sb.append(", and ").append(phrases.getLast());
		return sb.toString();
	}

	private static String softenWeaknessCue(String cue) {
		if (cue == null || cue.isBlank()) {
			return "";
		}
		String s = sanitizeSpaces(cue.replace(" — ", "; "));
		if (s.length() <= 160) {
			return lowerFirstLetter(s);
		}
		return lowerFirstLetter(s.substring(0, 157)).concat("…");
	}

	private static String lowerFirstLetter(String sentence) {
		if (sentence == null || sentence.isEmpty()) {
			return "";
		}
		char c0 = sentence.charAt(0);
		if (Character.isLowerCase(c0)) {
			return sentence;
		}
		return Character.toLowerCase(c0) + sentence.substring(1);
	}

	private static String growthAreaCue(String growthAreaLabel) {
		if (growthAreaLabel == null || growthAreaLabel.isBlank()) {
			return "";
		}
		String lc = growthAreaLabel.toLowerCase(Locale.ROOT);
		if (lc.contains("readme")) {
			return "README files still need richer setup or usage sections";
		}
		if (lc.contains("description")) {
			return "short descriptions still need another pass across several repos";
		}
		if (lc.contains("technologies") || lc.contains("variety")) {
			return "technology variety is still narrow across your portfolio repos";
		}
		if (lc.contains("active")) {
			return "several portfolio repos could use a fresher push";
		}
		if (lc.contains("original")) {
			return "more shipped original projects would make the narrative clearer";
		}
		return "";
	}

	private static String joinLanguageList(List<String> langs, int limit) {
		if (langs == null || langs.isEmpty()) {
			return "";
		}
		ArrayList<String> parts = new ArrayList<>(langs.subList(0, Math.min(langs.size(), limit)));
		return joinBulletsNatural(parts);
	}

	private static String buildProjectQualityExplanation(
			int portfolioRepoCount,
			int portfolioAverageRepoScore,
			int forkRepoCountInFetch) {
		if (portfolioRepoCount <= 0) {
			String forkHint =
					forkRepoCountInFetch > 0
							? "What we fetched leaned heavily on forks, so surfaced originals were thin."
							: "";
			String base =
					"We need at least one original, portfolio-style repo in the repos we reviewed before this score can lift off.";
			return forkHint.isEmpty() ? sentenceDot(base) : sentenceDot(base + " " + forkHint);
		}
		final String readmeSeparationFootnote =
				" README depth is summarized under README & documentation, not duplicated in this metric.";
		if (portfolioRepoCount == 1) {
			return sentenceDot(
					"Only one portfolio repo appeared in what we reviewed, so this score rides entirely on how polished that repo looks "
							+ "(description, stars, activity, completeness)."
							+ readmeSeparationFootnote);
		}
		String density = portfolioRepoCount < MIN_PORTFOLIO_REPO_COUNT
				? "With fewer than three portfolio repos opened, bonuses for breadth stay modest."
				: "Having several portfolio repos spreads the averages a bit.";
		density += readmeSeparationFootnote;
		if (portfolioAverageRepoScore >= 75) {
			return sentenceDot(
					"Your strongest repos average about "
							+ portfolioAverageRepoScore
							+ "/100 on repo quality across "
							+ portfolioRepoCount
							+ " portfolio repos, which looks consistently cared for. "
							+ density);
		}
		if (portfolioAverageRepoScore >= 55) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"Average repo quality sits near %d/100 across %d portfolio repos we reviewed—a steady baseline with room for polish. %s",
							portfolioAverageRepoScore,
							portfolioRepoCount,
							density));
		}
		return sentenceDot(
				String.format(Locale.ROOT,
						"Average repo quality is closer to %d/100 across %d portfolio repos, "
								+ "so descriptions, activity, or README touches could help the story land better.",
						portfolioAverageRepoScore,
						portfolioRepoCount));
	}

	private static String buildTechnicalBreadthExplanation(
			long distinctLangCount,
			List<String> topLanguages,
			boolean portfolioReposEmpty) {
		if (portfolioReposEmpty || distinctLangCount <= 0) {
			return sentenceDot(
					"We did not see multiple languages show up clearly across your portfolio repos in what we reviewed.");
		}
		if (distinctLangCount >= MIN_DISTINCT_LANGUAGES_PORTFOLIO) {
			String langs = joinLanguageList(topLanguages, MIN_DISTINCT_LANGUAGES_PORTFOLIO);
			return sentenceDot(
					String.format(Locale.ROOT,
							"Your portfolio shows work across %d languages (%s)—that reads like healthy variety.",
							distinctLangCount,
							langs.isEmpty() ? "several ecosystems" : langs));
		}
		String langsTwo = joinLanguageList(topLanguages, 2);
		if (distinctLangCount == 1) {
			if (langsTwo.isBlank()) {
				return sentenceDot(
						"Most of the reviewed work clusters around one main language—you could add another shipped project to widen variety.");
			}
			return sentenceDot(
					String.format(Locale.ROOT,
							"Most of the reviewed work centers on %s—you could add another project to show breadth.",
							langsTwo));
		}
		return sentenceDot(
				String.format(
						Locale.ROOT,
						"Two primary languages surfaced (%s). Adding another focus area alongside them would widen the footprint.",
						langsTwo.isBlank() ? "limited coverage so far" : langsTwo));
	}

	private static String buildDocumentationExplanation(
			int portfolioRepoCount,
			int documentationAvgScore,
			long readmeWeakCount,
			long readmeMissingCount) {
		if (portfolioRepoCount <= 0) {
			return sentenceDot(
					"We cannot score READMEs until there are portfolio repos to read; forks and demos are skipped in that average.");
		}
		if (readmeMissingCount == portfolioRepoCount) {
			return sentenceDot(
					"Several portfolio repos are missing README files entirely, which makes the profile harder to scan quickly.");
		}
		if (readmeWeakCount >= Math.max(1, portfolioRepoCount * 3 / 4)) {
			return sentenceDot(
					"Most portfolio READMEs lack setup clarity, concrete usage examples, or enough depth to onboard someone new.");
		}
		if (documentationAvgScore >= 70) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"README averages are strong (~%d/100) across portfolio repos, with substantive setup or story detail.",
							documentationAvgScore));
		}
		if (documentationAvgScore <= 42) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"README averages trail at ~%d/100—invest time in installation and usage sections readers can copy-paste.",
							documentationAvgScore));
		}
		return sentenceDot(
				String.format(Locale.ROOT,
						"README quality sits near ~%d/100—they explain basics but could use another editing pass.",
						documentationAvgScore));
	}

	private static String buildOriginalityExplanation(
			int originalRepoCount,
			int analyzedOriginalRepoCount,
			int portfolioRepoCount,
			int forkRepoCount,
			int demoRepoCount) {
		if (originalRepoCount <= 0) {
			return sentenceDot(
					"We only surfaced forks among the repos we fetched, "
							+ "so there is not much original project work left to emphasize.");
		}
		long nonPortfolioOriginalsApprox = Math.max(0L, analyzedOriginalRepoCount - portfolioRepoCount);
		if (forkRepoCount + demoRepoCount >= analyzedOriginalRepoCount && portfolioRepoCount <= 2) {
			return sentenceDot(
					"Many repos look like forks, demos, or samples, leaving fewer originals that feel like portfolio centerpieces.");
		}
		double coverage = analyzedOriginalRepoCount == 0
				? 0
				: (portfolioRepoCount * 100.0) / Math.max(analyzedOriginalRepoCount, 1);

		String tail = demoRepoCount > 0 ? " Demo-style repos still pull this reading downward a bit." : "";
		if (coverage >= 80 && portfolioRepoCount >= MIN_PORTFOLIO_REPO_COUNT) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"A large share (~%.0f%%) of the originals we opened read as genuine portfolio repos.%s",
							coverage,
							tail.isEmpty() ? "" : tail));
		}
		String gap = "";
		if (nonPortfolioOriginalsApprox > 0) {
			gap = String.format(Locale.ROOT,
					"%d originals still resemble forks or demos alongside your portfolio staples. ",
					nonPortfolioOriginalsApprox);
		}
		String lead = gap.isBlank() ? "" : gap.strip() + " ";
		return sentenceDot(String.format(Locale.ROOT,
				"%sThat leaves %d portfolio repos anchoring how original work reads.%s",
				lead,
				portfolioRepoCount,
				tail.isBlank() ? "" : tail));
	}

	private static String buildActivityExplanation(
			int portfolioRepoCount,
			int activityScore,
			boolean recentActivityStrength) {
		if (portfolioRepoCount <= 0) {
			return sentenceDot(
					"Recent activity waits on portfolio repos to measure—nothing to gauge yet.");
		}
		if (recentActivityStrength && activityScore >= 70) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"Around %d%% of portfolio repos landed recent updates—a healthy upkeep cadence.",
							activityScore));
		}
		if (recentActivityStrength) {
			return sentenceDot(
					"We saw at least one portfolio repo refreshed inside the recent window, which offsets quiet neighbors.");
		}
		if (activityScore >= 66) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"Around %d%% of portfolio repos show commits within roughly the last six months.",
							activityScore));
		}
		if (activityScore <= 33) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"Only about %d%% of portfolio repos updated recently—the rest look quiet lately.",
							activityScore));
		}
		return sentenceDot(
				String.format(Locale.ROOT,
						"Activity lands around ~%d%% overall—steady enough, but bursts of upkeep would brighten the pulse.",
						activityScore));
	}

	private static int computeSelectionScore(GitHubRepoDto repo, Instant sixMonthsAgo) {
		int score = 0;
		if (!repo.fork()) {
			score += 30;
		}
		if (repo.updatedAt() != null && !repo.updatedAt().isBefore(sixMonthsAgo)) {
			score += 25;
		}
		if (repo.language() != null && !repo.language().strip().isEmpty()) {
			score += 20;
		}
		String desc = repo.description();
		if (desc != null && desc.strip().length() >= STRONG_DESCRIPTION_MIN_LENGTH) {
			score += 15;
		}
		if (desc != null && desc.strip().length() >= 90) {
			score += 7;
		}
		if (!repo.topics().isEmpty() && repo.topics().stream().anyMatch(t -> t != null && !t.strip().isEmpty())) {
			score += 8;
		}
		if (repo.stars() >= 5) {
			score += 10;
		}
		else if (repo.stars() >= 1) {
			score += 5;
		}
		String name = repo.name() != null ? repo.name() : "";
		String descSafe = desc != null ? desc : "";
		if (isLikelyDemoRepo(name, descSafe)) {
			score -= 40;
		}
		if (repo.fork()) {
			score -= 30;
		}
		return score;
	}

	private static List<GitHubRepoDto> selectTopReposForDeepAnalysis(List<GitHubRepoDto> repos, Instant sixMonthsAgo) {
		return repos.stream()
				.filter(Objects::nonNull)
				.sorted(Comparator
						.comparingInt((GitHubRepoDto r) -> computeSelectionScore(r, sixMonthsAgo)).reversed()
						.thenComparing(Comparator.comparingInt(GitHubRepoDto::stars).reversed())
						.thenComparing(Comparator.comparing(
								(GitHubRepoDto r) -> r.updatedAt() != null ? r.updatedAt() : Instant.EPOCH).reversed())
						.thenComparing(r -> r.name() != null ? r.name().toLowerCase(Locale.ROOT) : ""))
				.limit(MAX_DEEP_ANALYSIS_REPOS)
				.toList();
	}

	private List<RepoAnalysisDto> buildRepoAnalyses(List<GitHubRepoDto> repos, Instant sixMonthsAgo, String ownerLogin) {
		return repos.stream()
				.filter(Objects::nonNull)
				.map(r -> toRepoAnalysis(r, sixMonthsAgo, ownerLogin))
				.toList();
	}

	private RepoAnalysisDto toRepoAnalysis(GitHubRepoDto r, Instant sixMonthsAgo, String ownerLogin) {
		boolean fork = r.fork();
		boolean originalRepo = !fork;
		String description = r.description();
		boolean hasDescription = description != null && !description.isBlank();
		boolean hasStrongDescription = hasDescription && description.strip().length() >= STRONG_DESCRIPTION_MIN_LENGTH;
		String language = r.language();
		boolean hasLanguage = language != null && !language.strip().isEmpty();
		Instant updated = r.updatedAt();
		boolean recentlyUpdated = updated != null && !updated.isBefore(sixMonthsAgo);

		boolean likelyDemoRepo =
				isLikelyDemoRepo(r.name() != null ? r.name() : "", description != null ? description : "");

		ReadmeAnalysisDto readmeAnalysis;
		boolean skipReadmeFetch = fork || likelyDemoRepo;
		if (skipReadmeFetch) {
			readmeAnalysis = analyzeReadme(null);
		}
		else {
			Optional<String> readmeBody =
					gitHubClient.fetchRepositoryReadmeContent(ownerLogin, r.name() != null ? r.name() : "");
			readmeAnalysis = analyzeReadme(readmeBody.orElse(null));
		}

		RepoScores scores = computeRepoScores(
				originalRepo, hasLanguage, hasStrongDescription, recentlyUpdated,
				r.stars(), likelyDemoRepo, fork, readmeAnalysis);

		List<String> repoSignals = buildRepoSignals(
				fork, originalRepo, hasLanguage, hasStrongDescription,
				recentlyUpdated, r.stars(), likelyDemoRepo,
				readmeAnalysis);

		String signal =
				resolveRepoQualitySignal(fork, likelyDemoRepo, readmeAnalysis.documentationScore(),
						hasStrongDescription, scores.compositeScore());

		return new RepoAnalysisDto(
				r.name(),
				description,
				language,
				r.stars(),
				r.forks(),
				hasDescription,
				recentlyUpdated,
				fork,
				originalRepo,
				hasLanguage,
				hasStrongDescription,
				likelyDemoRepo,
				readmeAnalysis,
				signal,
				scores.metadataScore(),
				scores.compositeScore(),
				List.copyOf(repoSignals)
		);
	}

	private static ReadmeAnalysisDto analyzeReadme(String rawContent) {
		if (rawContent == null || rawContent.isBlank()) {
			return new ReadmeAnalysisDto(false, 0, false, false, false, false, false, 0);
		}
		String text = rawContent.strip();
		int readmeLength = text.length();
		String lower = text.toLowerCase(Locale.ROOT);

		boolean hasInstallationInstructions =
				containsAny(lower, "installation", "install", "setup", "getting started");
		boolean hasUsageInstructions =
				containsAny(lower, "usage", "how to use", "run", "example");
		boolean hasTechStackMention =
				TECH_STACK_MARKERS.stream().anyMatch(lower::contains);
		boolean hasFeatureSection =
				containsAny(lower, "features", "functionality", "what it does");
		boolean hasScreenshots = text.contains("![");

		int documentationScore = 0;
		documentationScore += 10;
		if (readmeLength >= 500) {
			documentationScore += 20;
		}
		if (readmeLength >= 1000) {
			documentationScore += 10;
		}
		if (hasInstallationInstructions) {
			documentationScore += 15;
		}
		if (hasUsageInstructions) {
			documentationScore += 15;
		}
		if (hasTechStackMention) {
			documentationScore += 15;
		}
		if (hasFeatureSection) {
			documentationScore += 10;
		}
		if (hasScreenshots) {
			documentationScore += 5;
		}

		return new ReadmeAnalysisDto(
				true,
				readmeLength,
				hasInstallationInstructions,
				hasUsageInstructions,
				hasTechStackMention,
				hasFeatureSection,
				hasScreenshots,
				clampRepoScore(documentationScore));
	}

	private static boolean containsAny(String haystack, String... needles) {
		for (String needle : needles) {
			if (haystack.contains(needle)) {
				return true;
			}
		}
		return false;
	}

	private record RepoScores(int metadataScore, int compositeScore) {
	}

	/**
	 * Target interpretation: decent ~50–69, strong ~70–84, very strong ~85–94;
	 * scores near 100 require stacked signals — near-max docs blend plus exceptional star tier (+{@link #TOP_STAR_BUCKET_BONUS}), not hype alone on empty metadata.
	 */
	private static RepoScores computeRepoScores(
			boolean originalRepo,
			boolean hasLanguage,
			boolean hasStrongDescription,
			boolean recentlyUpdated,
			int stars,
			boolean likelyDemo,
			boolean fork,
			ReadmeAnalysisDto readme) {
		int meta = 0;
		if (originalRepo) {
			meta += RW_ORIGINAL_WEIGHT;
		}
		if (hasLanguage) {
			meta += RW_LANGUAGE_WEIGHT;
		}
		if (hasStrongDescription) {
			meta += RW_STRONG_DESCRIPTION_WEIGHT;
		}
		if (recentlyUpdated) {
			meta += RW_RECENCY_WEIGHT;
		}
		if (stars >= 10_000) {
			meta += TOP_STAR_BUCKET_BONUS;
		}
		else if (stars >= 2_500) {
			meta += 22;
		}
		else if (stars >= 1_000) {
			meta += 20;
		}
		else if (stars >= 500) {
			meta += 17;
		}
		else if (stars >= 250) {
			meta += 15;
		}
		else if (stars >= 100) {
			meta += 13;
		}
		else if (stars >= 50) {
			meta += 11;
		}
		else if (stars >= 25) {
			meta += 9;
		}
		else if (stars >= 10) {
			meta += 7;
		}
		else if (stars >= 5) {
			meta += 5;
		}
		else if (stars >= 1) {
			meta += 3;
		}

		int readmeContribution =
				readme.documentationScore() * README_SCORE_PERCENT_OF_REPO_TOTAL / 100;

		if (!readme.hasReadme() && !hasStrongDescription) {
			meta -= 25;
		}
		if (likelyDemo) {
			meta -= 30;
		}
		if (fork) {
			meta -= 25;
		}
		meta = clampRepoScore(meta);
		if (!readme.hasReadme() && !hasStrongDescription) {
			meta = Math.min(meta, 70);
		}
		if (!fork && likelyDemo) {
			meta = Math.min(meta, 60);
		}
		if (fork) {
			meta = Math.min(meta, 50);
		}

		int composite = clampRepoScore(meta + readmeContribution);
		if (!fork && likelyDemo) {
			composite = Math.min(composite, 60);
		}
		if (fork) {
			composite = Math.min(composite, 50);
		}
		return new RepoScores(meta, composite);
	}

	private static int clampRepoScore(int value) {
		return Math.min(100, Math.max(0, value));
	}

	private static List<String> buildRepoSignals(
			boolean fork,
			boolean originalRepo,
			boolean hasLanguage,
			boolean hasStrongDescription,
			boolean recentlyUpdated,
			int stars,
			boolean likelyDemoRepo,
			ReadmeAnalysisDto readmeAnalysis) {
		List<String> signals = new ArrayList<>();
		if (fork) {
			signals.add("Fork");
		}
		else if (originalRepo) {
			signals.add("Original repo");
		}
		if (likelyDemoRepo) {
			signals.add("Looks like a demo or sample");
		}
		appendRepoDocumentationSignals(signals, readmeAnalysis, hasStrongDescription);
		if (hasLanguage) {
			signals.add("Language listed");
		}
		if (hasStrongDescription) {
			signals.add("Clear description");
		}
		if (recentlyUpdated) {
			signals.add("Recently updated");
		}
		if (stars > 0) {
			signals.add("Has stars");
		}
		return signals;
	}

	private static void appendRepoDocumentationSignals(
			List<String> signals,
			ReadmeAnalysisDto readme,
			boolean hasStrongDescription) {
		if (!readme.hasReadme()) {
			signals.add("No README yet");
		}
		else {
			signals.add("README present");
			if (readme.hasInstallationInstructions()) {
				signals.add("README covers setup");
			}
			if (readme.hasUsageInstructions()) {
				signals.add("README covers usage");
			}
			if (readme.hasTechStackMention()) {
				signals.add("README mentions stack");
			}
			if (readme.hasScreenshots()) {
				signals.add("README has screenshots");
			}
		}
		int docScore = readme.documentationScore();
		if (docScore < 40) {
			signals.add("README could be stronger");
		}
		if (docScore >= 70) {
			signals.add("README looks thorough");
		}
		if (!hasStrongDescription) {
			signals.add("Could use a sharper description");
		}
	}

	private static boolean isLikelyDemoRepo(String name, String description) {
		String normalizedName = name == null ? "" : name.strip().toLowerCase(Locale.ROOT);
		if (DEMO_NAME_PATTERN.matcher(normalizedName).matches()) {
			return true;
		}
		String normalizedDescription = description == null ? "" : description.strip().toLowerCase(Locale.ROOT);
		for (String phrase : DEMO_DESCRIPTION_PHRASES) {
			if (normalizedDescription.contains(phrase)) {
				return true;
			}
		}
		return false;
	}

	private static String resolveRepoQualitySignal(
			boolean fork,
			boolean likelyDemoRepo,
			int documentationScore,
			boolean hasStrongDescription,
			int repoScore) {
		if (fork) {
			return "Fork";
		}
		if (likelyDemoRepo) {
			return "Looks like a demo or sample";
		}
		if (documentationScore == 0 && !hasStrongDescription) {
			return "Active, but README is thin";
		}
		if (repoScore >= 85) {
			return "Strong portfolio repo";
		}
		if (repoScore >= 70) {
			return "Solid project";
		}
		if (repoScore >= 50) {
			return "Basic project";
		}
		return "Room to strengthen this repo";
	}

	private static Map<String, Long> portfolioLanguageCounts(List<RepoAnalysisDto> portfolioRepos) {
		Map<String, Long> counts = new LinkedHashMap<>();
		for (RepoAnalysisDto r : portfolioRepos) {
			if (!r.hasLanguage() || r.language() == null) {
				continue;
			}
			String key = r.language().strip();
			if (key.isEmpty()) {
				continue;
			}
			counts.merge(key, 1L, Long::sum);
		}
		return counts;
	}

	private static List<GitHubRepoDto> safeRepos(List<GitHubRepoDto> repos) {
		return repos != null ? repos : List.of();
	}

	private static long distinctLanguageCount(Map<String, Long> languageCounts) {
		return languageCounts.size();
	}

	private static List<String> topLanguagesDescending(Map<String, Long> counts) {
		return counts.entrySet().stream()
				.sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
						.thenComparing(Map.Entry::getKey, Comparator.comparing(s -> s.toLowerCase(Locale.ROOT))))
				.map(Map.Entry::getKey)
				.toList();
	}

	private static Instant sixMonthsAgoUtcInstant() {
		return ZonedDateTime.now(ZoneOffset.UTC).minusMonths(6).toInstant();
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
