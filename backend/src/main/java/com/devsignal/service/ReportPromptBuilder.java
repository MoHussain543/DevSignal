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
			The user already sees a separate scored report with numeric breakdowns and tactical bullet lists.
			Your job is a rich interpretive AI report — synthesis, evidence, and prioritization.
			Do NOT duplicate the scored report: no category score tables, no repeated strength/growth bullet lists.

			Rules:
			- Use ONLY facts present in the user message. Do not invent repos, scores, languages, or metrics.
			- Do not recalculate or change the overall score.
			- Do not output strengths[] or weaknesses[] arrays that mirror the tactical section.
			- Do not mention internal field names or JSON.
			- Keep tone polished, human, and practical — not robotic or heavy recruiter jargon.
			- Each field must be distinct and non-overlapping.
			- Verdict row fields must be short scannable phrases (roughly 4–14 words each), not paragraphs.
			- Evidence signal lists: 2–5 items each, human-readable phrases grounded in the analysis (no raw field names).
			- topPriorities: exactly 3 items, ranked most important first; tactical and near-term (not the same as biggestUnlock).
			- biggestUnlock: one strategic higher-leverage change; broader than any single priority.
			- Narrative prose fields: no bullet points, no markdown headers, no leading dashes inside string values.
			- Respond with a single JSON object only, no markdown fences, matching this schema exactly:
			  {
			    "overallSummary": "4-6 sentences: the whole picture at a glance — how the portfolio reads end-to-end",
			    "overallRead": "short phrase for overall portfolio read, e.g. Promising but uneven",
			    "hiringSignal": "short phrase for hiring-readiness impression, e.g. Moderate portfolio signal",
			    "mainGap": "short phrase for the main weakness, e.g. README clarity",
			    "bestSignal": "short phrase for the strongest positive, e.g. Original project activity",
			    "howProfileReadsPattern": "2-3 sentences: what pattern the portfolio gives off overall",
			    "howProfileReadsDrivers": "2-3 sentences: what is driving that perception (interpretation, not score repetition)",
			    "howProfileReadsHelps": "2-3 sentences: what is helping the profile",
			    "howProfileReadsHoldsBack": "2-3 sentences: what is holding it back",
			    "hiringImpression": "4-6 sentences: what builds confidence, what creates hesitation, how a thoughtful reviewer would read this profile",
			    "whatStandsOut": "4-5 sentences: the strongest visible positive pattern, why it matters, how it affects perception",
			    "whatWeakens": "4-5 sentences: the main thing reducing impact, why it hurts, how it changes the overall read",
			    "positiveSignals": ["evidence item 1", "evidence item 2"],
			    "warningSignals": ["evidence item 1", "evidence item 2"],
			    "missingSignals": ["evidence item 1", "evidence item 2"],
			    "topPriorities": [
			      {
			        "action": "the concrete action to take",
			        "whyItMatters": "why this matters for portfolio perception",
			        "visibleImprovement": "what visible GitHub improvement this would create"
			      }
			    ],
			    "biggestUnlock": "4-6 sentences: the single larger strategic change that would most improve the portfolio overall"
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

		sb.append("Category scores (0-100) — for grounding only; do NOT repeat as a breakdown section:\n");
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

		sb.append("Tactical bullets (already on scored report — use for evidence grounding, do not repeat verbatim):\n");
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
