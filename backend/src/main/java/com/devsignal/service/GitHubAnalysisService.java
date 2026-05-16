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
					"Original project signals are limited among fetched repositories — every analyzed repository appears to be a fork");
		}

		if (portfolioRepos.isEmpty()) {
			weaknesses.add("No strong original portfolio repositories detected in the analyzed sample");
		}
		else {
			if (repoCountStrength) {
				strengths.add("Multiple qualifying portfolio repositories in the analyzed sample");
			}
			else {
				weaknesses.add("Fewer than three qualifying original repositories in the analyzed portfolio sample");
			}
			if (starsStrength) {
				strengths.add("At least one analyzed repository has stars");
			}
			else {
				weaknesses.add("Portfolio repositories in the analyzed sample have no stars yet");
			}
			if (languagesStrength) {
				strengths.add("Uses several programming languages across analyzed portfolio repositories");
			}
			else {
				weaknesses.add("Limited language variety among analyzed portfolio repositories");
			}
			if (descriptionStrength) {
				strengths.add("Most qualifying analyzed repositories include strong descriptions");
			}
			else {
				weaknesses.add("Many analyzed repositories are missing strong descriptions");
			}
			if (recentActivityStrength) {
				strengths.add("Most qualifying analyzed portfolio repositories were updated recently");
			}
			else {
				weaknesses.add("No qualifying repositories in the analyzed sample were updated in the last 6 months");
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
				"Highest readiness score among analyzed original, non-demo portfolio repositories; "
						+ "tie-breaks favor README depth signals, recent updates, then star count.";
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
			return "Strong candidate";
		}
		if (score >= 70) {
			return "Promising candidate";
		}
		if (score >= 50) {
			return "Developing candidate";
		}
		return "Early portfolio";
	}

	private static String resolveHiringRecommendation(int score) {
		if (score >= 85) {
			return "Strong GitHub portfolio signal";
		}
		if (score >= 70) {
			return "Promising signal with some areas to improve";
		}
		if (score >= 50) {
			return "Some useful signals, but needs stronger project depth";
		}
		return "Needs stronger original projects before being hiring-ready";
	}

	private static List<String> buildTechnicalHighlights(
			boolean repoCountStrength,
			boolean recentActivityStrength,
			boolean starsStrength,
			boolean languagesStrength,
			boolean descriptionStrength) {
		List<String> highlights = new ArrayList<>();
		if (repoCountStrength) {
			highlights.add("Analyzed sample includes multiple qualifying original portfolio repositories");
		}
		if (recentActivityStrength) {
			highlights.add("Most qualifying analyzed portfolio repositories were updated recently");
		}
		if (starsStrength) {
			highlights.add("At least one analyzed repository has stars");
		}
		if (languagesStrength) {
			highlights.add("Multiple languages appear across analyzed portfolio repositories");
		}
		if (descriptionStrength) {
			highlights.add("Most qualifying analyzed repositories include strong descriptions");
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
			areas.add("Build more original non-demo projects");
		}
		else if (!repoCountStrength) {
			areas.add("Build more original non-demo projects");
		}
		if (!portfolioEmpty && !descriptionStrength) {
			areas.add("Improve repository descriptions");
			areas.add("Add clearer documentation");
		}
		else if (portfolioEmpty) {
			areas.add("Improve repository descriptions");
			areas.add("Add clearer documentation");
		}
		if (!portfolioEmpty && !languagesStrength) {
			areas.add("Add more language/technology variety in analyzed portfolio repositories");
		}
		else if (portfolioEmpty) {
			areas.add("Add more language/technology variety");
		}
		if (!portfolioEmpty && !recentActivityStrength) {
			areas.add("Keep analyzed portfolio repositories active");
		}
		else if (portfolioEmpty) {
			areas.add("Keep analyzed portfolio repositories active");
		}
		appendBreakdownGrowthAreas(areas, scoreBreakdown);
		return areas;
	}

	private static void appendBreakdownStrengths(List<String> strengths, ScoreBreakdownDto breakdown) {
		if (breakdown.documentationScore() >= 70) {
			addOnce(strengths, "Strong README/documentation quality across analyzed portfolio repositories");
		}
		if (breakdown.originalityScore() >= 70) {
			addOnce(strengths, "Strong originality among qualifying repositories in the analyzed sample");
		}
		if (breakdown.technicalBreadthScore() >= 70) {
			addOnce(strengths, "Strong detected technical breadth across the analyzed portfolio sample");
		}
	}

	private static void appendBreakdownGrowthAreas(List<String> areas, ScoreBreakdownDto breakdown) {
		if (breakdown.documentationScore() < 40) {
			addOnce(areas, "Improve README/documentation quality");
		}
		if (breakdown.originalityScore() < 40) {
			addOnce(areas, "Build more original non-demo projects");
		}
		if (breakdown.technicalBreadthScore() < 40) {
			addOnce(
					areas,
					"Increase detected technical breadth with more languages or stacks among analyzed repositories");
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
			Comparator.comparingInt(GitHubAnalysisService::repoDisplayTier)
					.thenComparing(Comparator.comparingInt(RepoAnalysisDto::repoScore).reversed())
					.thenComparing(r -> r.name() == null ? "" : r.name(), String.CASE_INSENSITIVE_ORDER);

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
						" because every fetched repository appears to be a fork and ")
						.append("no qualifying original portfolio repositories were analyzed");
			}
			else {
				sb.append(" because ")
						.append("no qualifying original, non-sample portfolio repositories were ")
						.append("detected among the repositories analyzed");
			}
			return sentenceDot(sb.toString());
		}

		ArrayList<String> positives = new ArrayList<>();
		if (recentActivityStrength) {
			positives.add("it shows recent activity on qualifying portfolio repositories");
		}
		if (weighted.projectQualityPoints() >= 20 && portfolioRepoCount >= MIN_PORTFOLIO_REPO_COUNT) {
			positives.add("project readiness averages strong across analyzed qualifying repositories");
		}
		else if (portfolioAveragePassesThreshold(portfolioRepoCount, weighted.projectQualityPoints())) {
			positives.add("the strongest repos pull up averaged portfolio quality signals in the analyzed sample");
		}
		if (b.documentationScore() >= 60) {
			positives.add("README-backed documentation averages well among analyzed portfolio repositories");
		}
		else if (descriptionStrength) {
			positives.add("some analyzed repositories include solid descriptions");
		}
		if (b.technicalBreadthScore() >= 68 && portfolioRepoCount > 0) {
			positives.add("multiple primary languages surface across analyzed qualifying repositories");
		}
		if (b.originalityScore() >= 65) {
			positives.add("most analyzed work skews toward original projects versus forks");
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
					.append(" with fewer obvious structural gaps among the analyzed repositories");
		}
		else {
			sb.append("the averaged repository signals hover near middling ")
					.append("without a sharp standout pillar in the analyzed sample");
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
				addPhraseIfNew(phrases, "only one qualifying portfolio repository was detected");
			}
			else if (portfolioRepoCount == 0) {
				addPhraseIfNew(phrases, "no qualifying portfolio repositories were detected");
			}
			else {
				addPhraseIfNew(phrases, "fewer than three qualifying portfolio repositories were surfaced");
			}
		}
		if (b.projectQualityScore() < 48 && portfolioRepoCount > 0) {
			addPhraseIfNew(
					phrases,
					"overall project quality averages below what hiring teams typically highlight");
		}
		if (b.technicalBreadthScore() <= 44) {
			if (portfolioRepoCount <= 1) {
				addPhraseIfNew(
						phrases,
						"detected technical breadth is limited because qualifying analyzed repos show thin language variety");
			}
			else {
				addPhraseIfNew(
						phrases,
						"detected technical breadth stays limited versus candidates with richer language variety in their analyzed sample");
			}
		}
		if (b.documentationScore() <= 42) {
			addPhraseIfNew(
					phrases,
					"documentation is weaker because averaged README signals in the analyzed repos miss setup depth or usage detail");
		}
		else if (!descriptionStrength && b.documentationScore() < 62) {
			addPhraseIfNew(
					phrases,
					"few analyzed repositories pair strong blurbs with deep README scaffolding");
		}
		if (b.originalityScore() <= 40) {
			addPhraseIfNew(
					phrases,
					"originality dips when many repositories in the analyzed sample resemble forks, demos, or samples");
		}
		if (b.activityScore() <= 42 && !recentActivityStrength && portfolioRepoCount > 0) {
			addPhraseIfNew(
					phrases,
					"activity is muted—qualifying analyzed repos mostly missed updates in the last six months");
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
		if (lc.contains("documentation")) {
			return "documentation can still deepen with richer README sections";
		}
		if (lc.contains("language")) {
			return "technology breadth remains narrow across analyzed portfolio repositories";
		}
		if (lc.contains("active")) {
			return "recent maintainership on analyzed portfolio repositories remains thin";
		}
		if (lc.contains("original")) {
			return "more shipped original projects would raise confidence";
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
							? "The analyzed sample leaned on forks instead of surfaced originals."
							: "";
			String base =
					"Project quality stays undefined until at least one original, non-sample portfolio repo appears in the analyzed repositories.";
			return forkHint.isEmpty() ? sentenceDot(base) : sentenceDot(base + " " + forkHint);
		}
		final String readmeSeparationFootnote =
				" README quality is summarized in the Documentation pillar, not duplicated in this pillar average.";
		if (portfolioRepoCount == 1) {
			return sentenceDot(
					"Project quality is limited because only one qualifying portfolio repository was detected in the analyzed sample,"
							+ " so averages hinge on that single project's metadata readiness (stars, blurbs, activity)."
							+ readmeSeparationFootnote);
		}
		String density = portfolioRepoCount < MIN_PORTFOLIO_REPO_COUNT
				? "Fewer than three qualifying repositories in the analyzed sample keep the breadth bonus modest."
				: "Several qualifying analyzed repositories help diversify the averaged signal.";
		density += readmeSeparationFootnote;
		if (portfolioAverageRepoScore >= 75) {
			return sentenceDot(
					"Portfolio metadata readiness benchmarks high because averaging "
							+ portfolioAverageRepoScore
							+ "/100 across "
							+ portfolioRepoCount
							+ " qualifying analyzed repositories aligns with repeatable execution. "
							+ density);
		}
		if (portfolioAverageRepoScore >= 55) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"Averaged metadata readiness sits near %d/100 across %d analyzed repositories, implying steady but improvable craftsmanship. %s",
							portfolioAverageRepoScore,
							portfolioRepoCount,
							density));
		}
		return sentenceDot(
				String.format(Locale.ROOT,
						"Averaged metadata readiness lingers closer to %d/100 across %d qualifying analyzed repositories,"
								+ " so reviewers see uneven polish until descriptions, stars, and activity reinforce the story.",
						portfolioAverageRepoScore,
						portfolioRepoCount));
	}

	private static String buildTechnicalBreadthExplanation(
			long distinctLangCount,
			List<String> topLanguages,
			boolean portfolioReposEmpty) {
		if (portfolioReposEmpty || distinctLangCount <= 0) {
			return sentenceDot(
					"Detected technical breadth is muted because qualifying portfolio repositories in the analyzed sample did not surface multiple primary languages.");
		}
		if (distinctLangCount >= MIN_DISTINCT_LANGUAGES_PORTFOLIO) {
			String langs = joinLanguageList(topLanguages, MIN_DISTINCT_LANGUAGES_PORTFOLIO);
			return sentenceDot(
					String.format(Locale.ROOT,
							"Detected technical breadth looks wide with %d distinct languages (%s)"
									+ " represented across qualifying analyzed portfolio repositories.",
							distinctLangCount,
							langs.isEmpty() ? "several ecosystems" : langs));
		}
		String langsTwo = joinLanguageList(topLanguages, 2);
		if (distinctLangCount == 1) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"Detected technical breadth is limited because analyzed portfolio repositories largely share one detected primary%s.",
							langsTwo.isBlank() ? " language"
									: " language highlighted as ".concat(langsTwo)));
		}
		return sentenceDot(
					String.format(Locale.ROOT,
							"Detected technical breadth improves slowly with only two primary languages highlighted (%s),"
									+ " versus profiles that diversify further in the analyzed sample.",
							langsTwo.isBlank() ? "limited coverage" : langsTwo));
	}

	private static String buildDocumentationExplanation(
			int portfolioRepoCount,
			int documentationAvgScore,
			long readmeWeakCount,
			long readmeMissingCount) {
		if (portfolioRepoCount <= 0) {
			return sentenceDot(
					"Documentation cannot score until qualifying analyzed repositories exist; forks and demos are skipped for averaged README reads.");
		}
		if (readmeMissingCount == portfolioRepoCount) {
			return sentenceDot(
					"Documentation reads as weak because READMEs were missing entirely on qualifying analyzed portfolio repositories.");
		}
		if (readmeWeakCount >= Math.max(1, portfolioRepoCount * 3 / 4)) {
			return sentenceDot(
					"Documentation is weak because most qualifying analyzed repositories miss README depth, setup scaffolding, or usage walkthroughs.");
		}
		if (documentationAvgScore >= 70) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"Documentation averages a strong README signal (~%d/100)"
									+ " across qualifying analyzed portfolio repositories with substantive setup or narrative detail.",
							documentationAvgScore));
		}
		if (documentationAvgScore <= 42) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"Documentation trails peers at ~%d/100 averages because summaries rarely pair deep setup or usage detail.",
							documentationAvgScore));
		}
		return sentenceDot(
				String.format(Locale.ROOT,
						"Documentation sits near ~%d/100 averages, meaning READMEs partially explain architecture but lack polish.",
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
					"Originality is constrained because fetched repositories all appear forked,"
							+ " so originality metrics cannot emphasize personal builds.");
		}
		long nonPortfolioOriginalsApprox = Math.max(0L, analyzedOriginalRepoCount - portfolioRepoCount);
		if (forkRepoCount + demoRepoCount >= analyzedOriginalRepoCount && portfolioRepoCount <= 2) {
			return sentenceDot(
					"Originality is limited because many repositories resemble forks, demos, or curated samples,"
							+ " leaving fewer surfaced originals trusted as portfolio staples.");
		}
		double coverage = analyzedOriginalRepoCount == 0
				? 0
				: (portfolioRepoCount * 100.0) / Math.max(analyzedOriginalRepoCount, 1);

		String tail = demoRepoCount > 0 ? " Sample-style repos still blunt the originality reading." : "";
		if (coverage >= 80 && portfolioRepoCount >= MIN_PORTFOLIO_REPO_COUNT) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"Originality is solid because %.0f percent of surfaced originals classify as qualifying portfolio repos.%s",
							coverage,
							tail.isEmpty() ? "" : tail));
		}
		String gap = "";
		if (nonPortfolioOriginalsApprox > 0) {
			gap = String.format(Locale.ROOT,
					"%d surfaced originals behave like forks, demos, or otherwise non-qualifying work. ",
					nonPortfolioOriginalsApprox);
		}
		return sentenceDot(
				String.format(Locale.ROOT,
						"Originality is mixed once forks and demos are filtered out;"
								+ " %sit leaves %d qualifying originals that anchor the averaged score.%s",
						gap,
						portfolioRepoCount,
						tail.isBlank() ? "" : tail));
	}

	private static String buildActivityExplanation(
			int portfolioRepoCount,
			int activityScore,
			boolean recentActivityStrength) {
		if (portfolioRepoCount <= 0) {
			return sentenceDot(
					"Activity stays neutral until there are qualifying analyzed repositories to inspect for recent updates.");
		}
		if (recentActivityStrength && activityScore >= 70) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"Activity looks strong across %d percent of qualifying analyzed portfolio repositories after recent updates surfaced.",
							activityScore));
		}
		if (recentActivityStrength) {
			return sentenceDot(
					"Activity is supported because at least one qualifying analyzed portfolio repository was updated recently within the monitored window.");
		}
		if (activityScore >= 66) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"Healthy maintainership signals appear with %d percent of analyzed qualifying repositories showing fresh commits.",
							activityScore));
		}
		if (activityScore <= 33) {
			return sentenceDot(
					String.format(Locale.ROOT,
							"Activity sits weak at roughly %d percent because most qualifying analyzed portfolio repositories stalled before the six-month cutoff.",
							activityScore));
		}
		return sentenceDot(
				String.format(Locale.ROOT,
						"Activity is middling (~%d percent) relative to analyzed qualifying repositories that refreshed inside the monitored window.",
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
			signals.add("Forked repository");
		}
		else if (originalRepo) {
			signals.add("Original repository");
		}
		if (likelyDemoRepo) {
			signals.add("Likely demo/sample repository");
		}
		appendRepoDocumentationSignals(signals, readmeAnalysis, hasStrongDescription);
		if (hasLanguage) {
			signals.add("Uses a detected primary language");
		}
		if (hasStrongDescription) {
			signals.add("Has a strong description");
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
			signals.add("Missing README");
		}
		else {
			signals.add("Has README");
			if (readme.hasInstallationInstructions()) {
				signals.add("README explains setup");
			}
			if (readme.hasUsageInstructions()) {
				signals.add("README explains usage");
			}
			if (readme.hasTechStackMention()) {
				signals.add("README mentions tech stack");
			}
			if (readme.hasScreenshots()) {
				signals.add("README includes screenshots");
			}
		}
		int docScore = readme.documentationScore();
		if (docScore < 40) {
			signals.add("Weak documentation");
		}
		if (docScore >= 70) {
			signals.add("Strong documentation");
		}
		if (!hasStrongDescription) {
			signals.add("Missing strong description");
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
			return "Forked repository";
		}
		if (likelyDemoRepo) {
			return "Likely demo/sample repository";
		}
		if (documentationScore == 0 && !hasStrongDescription) {
			return "Active project but missing documentation";
		}
		if (repoScore >= 85) {
			return "Strong portfolio project signal";
		}
		if (repoScore >= 70) {
			return "Promising project";
		}
		if (repoScore >= 50) {
			return "Basic project signal";
		}
		return "Needs stronger project signals";
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
