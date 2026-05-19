package com.devsignal.service;

import java.util.List;
import java.util.stream.Collectors;

import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.dto.analysis.RepoAnalysisDto;
import com.devsignal.dto.analysis.ScoreBreakdownDto;

/** Builds grounded prompts from existing analysis output — no new metrics. */
public final class ReportPromptBuilder {

	private static final String SYSTEM_PROMPT = """
			You are a helpful editorial assistant for DevSignal, a GitHub portfolio reviewer.
			You receive structured analysis that was already computed by deterministic rules.
			The user already sees a separate tactical section with strength and growth bullet lists.
			Your job is a higher-level strategic narrative — do NOT repeat those bullets or mirror that section.

			Rules:
			- Use ONLY facts present in the user message. Do not invent repos, scores, languages, or metrics.
			- Do not recalculate or change the overall score.
			- Do not output strengths[] or weaknesses[] arrays or bullet lists of gaps/strengths.
			- Do not mention internal field names or JSON.
			- Keep tone polished, human, and practical — not robotic or heavy recruiter jargon.
			- Each field must be distinct and non-overlapping:
			    overallSummary = the whole picture at a glance (how the portfolio reads end-to-end)
			    hiringImpression = how a thoughtful hiring reviewer would interpret this profile
			    whatStandsOut = the single most defining positive or noteworthy signal
			    whatWeakens = the most significant thing that undercuts the portfolio's credibility or appeal
			    improveFirst = the one immediate practical action that would have the most visible impact
			    biggestUnlock = the higher-leverage strategic change (may be broader or longer-term than improveFirst)
			- Write in flowing prose — no bullet points, no headers, no dashes inside field values.
			- Respond with a single JSON object only, no markdown fences, matching this schema exactly:
			  {
			    "overallSummary": "3-5 sentences on how the profile reads overall",
			    "hiringImpression": "3-5 sentences, thoughtful hiring-style interpretation",
			    "whatStandsOut": "2-3 sentences on the most noticeable positive or defining signal",
			    "whatWeakens": "2-3 sentences on the most significant gap or credibility risk",
			    "improveFirst": "2-3 sentences on the one immediate practical action with the biggest visible impact",
			    "biggestUnlock": "2-3 sentences on the higher-leverage strategic portfolio change"
			  }
			""";

	private ReportPromptBuilder() {
	}

	public static String systemPrompt() {
		return SYSTEM_PROMPT;
	}

	public static String userPrompt(AnalysisResponseDto analysis) {
		ScoreBreakdownDto b = analysis.scoreBreakdown();
		StringBuilder sb = new StringBuilder();
		sb.append("GitHub user: @").append(nullToEmpty(analysis.username())).append('\n');
		if (analysis.name() != null && !analysis.name().isBlank()) {
			sb.append("Display name: ").append(analysis.name().strip()).append('\n');
		}
		if (analysis.bio() != null && !analysis.bio().isBlank()) {
			sb.append("Bio: ").append(analysis.bio().strip()).append('\n');
		}
		sb.append("Overall score: ").append(analysis.score()).append("/100\n");
		sb.append("Portfolio signal label: ").append(nullToEmpty(analysis.candidateLevel())).append('\n');
		sb.append("Recommendation line: ").append(nullToEmpty(analysis.hiringRecommendation())).append('\n');
		sb.append('\n');

		sb.append("Repos reviewed: ").append(analysis.analyzedRepoCount());
		sb.append(" (fetched ").append(analysis.totalFetchedRepoCount());
		sb.append(", ignored ").append(analysis.ignoredRepoCount()).append(")\n");
		sb.append("Original repos: ").append(analysis.originalRepoCount());
		sb.append(", forks: ").append(analysis.forkedRepoCount()).append('\n');
		sb.append("Portfolio repos (original, non-demo): ").append(analysis.portfolioRepoCount()).append('\n');
		sb.append("Portfolio stars: ").append(analysis.portfolioTotalStars());
		sb.append(", forks: ").append(analysis.portfolioTotalForks()).append('\n');
		sb.append("Average repo quality (portfolio): ").append(analysis.portfolioAverageRepoScore()).append("/100\n");
		sb.append("Top languages: ").append(joinList(analysis.portfolioTopLanguages())).append('\n');
		sb.append('\n');

		sb.append("Category scores (0-100):\n");
		sb.append("- Project quality: ").append(b.projectQualityScore()).append('\n');
		sb.append("- Technology variety: ").append(b.technicalBreadthScore()).append('\n');
		sb.append("- README & documentation: ").append(b.documentationScore()).append('\n');
		sb.append("- Original project work: ").append(b.originalityScore()).append('\n');
		sb.append("- Recent activity: ").append(b.activityScore()).append('\n');
		sb.append('\n');

		sb.append("Score explanation: ").append(nullToEmpty(analysis.scoreExplanation())).append('\n');
		sb.append("Project quality note: ").append(nullToEmpty(analysis.projectQualityExplanation())).append('\n');
		sb.append("Technology variety note: ").append(nullToEmpty(analysis.technicalBreadthExplanation())).append('\n');
		sb.append("Documentation note: ").append(nullToEmpty(analysis.documentationExplanation())).append('\n');
		sb.append("Original work note: ").append(nullToEmpty(analysis.originalityExplanation())).append('\n');
		sb.append("Activity note: ").append(nullToEmpty(analysis.activityExplanation())).append('\n');
		sb.append('\n');

		sb.append("Context for grounding only (already shown elsewhere as tactical bullets — do not repeat as lists):\n");
		sb.append("Highlights:\n");
		appendBullets(sb, analysis.technicalHighlights());
		sb.append("Growth areas:\n");
		appendBullets(sb, analysis.growthAreas());
		sb.append('\n');

		if (analysis.featuredRepo() != null) {
			RepoAnalysisDto featured = analysis.featuredRepo();
			sb.append("Best portfolio repo: ").append(featured.name());
			sb.append(" (score ").append(featured.repoScore()).append("/100");
			if (featured.language() != null && !featured.language().isBlank()) {
				sb.append(", ").append(featured.language());
			}
			sb.append(", stars ").append(featured.stars()).append(")\n");
			if (analysis.featuredRepoReason() != null) {
				sb.append("Why featured: ").append(analysis.featuredRepoReason()).append('\n');
			}
		}

		sb.append("\nRepositories reviewed (name | score | language | stars | flags):\n");
		for (RepoAnalysisDto repo : analysis.repos()) {
			sb.append("- ").append(repo.name())
					.append(" | ").append(repo.repoScore()).append('/').append(100)
					.append(" | ").append(repo.language() == null ? "—" : repo.language())
					.append(" | stars ").append(repo.stars());
			if (repo.fork()) {
				sb.append(" | fork");
			}
			if (repo.likelyDemoRepo()) {
				sb.append(" | demo/sample");
			}
			if (repo.recentlyUpdated()) {
				sb.append(" | recent activity");
			}
			sb.append(" | ").append(repo.repoQualitySignal());
			sb.append('\n');
		}

		return sb.toString();
	}

	private static void appendBullets(StringBuilder sb, List<String> items) {
		if (items == null || items.isEmpty()) {
			sb.append("- (none listed)\n");
			return;
		}
		for (String item : items) {
			sb.append("- ").append(item).append('\n');
		}
	}

	private static String joinList(List<String> items) {
		if (items == null || items.isEmpty()) {
			return "(none)";
		}
		return items.stream().collect(Collectors.joining(", "));
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value.strip();
	}
}
