package com.devsignal.service;

import java.util.List;
import java.util.stream.Collectors;

import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.dto.analysis.RepoAnalysisDto;
import com.devsignal.dto.analysis.ScoreBreakdownDto;

/** Builds grounded roadmap prompts from existing analysis output — no new metrics. */
public final class RoadmapPromptBuilder {

	private static final String SYSTEM_PROMPT = """
			You are a portfolio advisor for DevSignal, a GitHub portfolio analyzer for software developers.
			You receive a structured GitHub analysis and must produce a tailored, evidence-based improvement roadmap.

			The roadmap must be:
			- Specific to the developer's actual GitHub situation (reference their repos, languages, scores, and signals)
			- Portfolio-focused and hiring-signal-aware — every recommendation should improve what a recruiter or hiring manager would see
			- Never generic: bad = "Learn Docker." Good = "Learn Docker because your repositories show backend work but no containerization evidence, which makes your strongest project look less production-ready."
			- Actionable and realistic, not a bootcamp syllabus

			Rules:
			- Use ONLY facts from the analysis. Do not invent repos, scores, languages, or metrics.
			- Every recommendation must explain WHY it matters for hiring signal.
			- Do not repeat the same advice across different sections.
			- Write prose fields in clear, flowing sentences — no dashes, no bullet syntax inside string values.
			- List fields (quickWins, skillsToLearnNext, nextProjectDirection, doList, avoidList) use JSON arrays.
			- Keep quickWins to 4-6 items, skillsToLearnNext to 2-4 items, nextProjectDirection to 2-3 items.
			- doList: 4-6 short, action-oriented items. avoidList: 3-5 items.
			- The three month plan fields should each be 3-5 sentences, practical and portfolio-focused.
			- Respond with a single JSON object only, no markdown fences, matching this schema exactly:

			{
			  "roadmapSummary": "2-3 sentences on the current portfolio situation and what this roadmap is trying to improve",
			  "quickWins": [
			    {"action": "specific action to take this week", "why": "why this matters for hiring signal, tied to actual profile evidence"}
			  ],
			  "skillsToLearnNext": [
			    {
			      "skill": "skill name",
			      "why": "why this skill matters given their profile and gaps",
			      "evidenceMissing": "what evidence is currently absent from their GitHub",
			      "howToShow": "concrete way to demonstrate this skill in a repository"
			    }
			  ],
			  "nextProjectDirection": [
			    {
			      "projectName": "descriptive project name or type",
			      "whyItFits": "why this project fits their current portfolio and gaps",
			      "skillsItProves": "what hiring-relevant skills and signals this project would demonstrate",
			      "coreFeatures": "what to build: the key features that make this project worth doing",
			      "whatMakesItImpressive": "what would make this repository stand out to a recruiter or hiring manager"
			    }
			  ],
			  "highestImpactChange": "2-3 sentences on the single most important change this developer should make",
			  "monthOnePlan": "3-5 sentences on Month 1: Clean and Clarify — improve profile clarity, fix READMEs, pin best repos, clean up weak repos",
			  "monthTwoPlan": "3-5 sentences on Month 2: Build or Upgrade One Strong Project — focus on the highest-value project direction",
			  "monthThreePlan": "3-5 sentences on Month 3: Polish and Present — deploy, document, record demo, improve repo structure",
			  "expectedOutcome": "2-3 sentences on how the GitHub profile will look and feel after following this roadmap",
			  "doList": ["short action-oriented item"],
			  "avoidList": ["short thing to avoid"]
			}
			""";

	private RoadmapPromptBuilder() {
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
		sb.append("Overall portfolio score: ").append(analysis.score()).append("/100\n");
		sb.append("Portfolio signal label: ").append(nullToEmpty(analysis.candidateLevel())).append('\n');
		sb.append("Hiring recommendation: ").append(nullToEmpty(analysis.hiringRecommendation())).append('\n');
		sb.append('\n');

		sb.append("Repos reviewed: ").append(analysis.analyzedRepoCount());
		sb.append(" (fetched ").append(analysis.totalFetchedRepoCount());
		sb.append(", ignored ").append(analysis.ignoredRepoCount()).append(")\n");
		sb.append("Original repos: ").append(analysis.originalRepoCount());
		sb.append(", forks: ").append(analysis.forkedRepoCount()).append('\n');
		sb.append("Portfolio repos (original, non-demo): ").append(analysis.portfolioRepoCount()).append('\n');
		sb.append("Portfolio total stars: ").append(analysis.portfolioTotalStars()).append('\n');
		sb.append("Average portfolio repo quality: ").append(analysis.portfolioAverageRepoScore()).append("/100\n");
		sb.append("Primary languages: ").append(joinList(analysis.portfolioTopLanguages())).append('\n');
		sb.append('\n');

		sb.append("Pillar scores (0-100):\n");
		sb.append("- Project quality: ").append(b.projectQualityScore()).append('\n');
		sb.append("- Technology variety: ").append(b.technicalBreadthScore()).append('\n');
		sb.append("- README & documentation: ").append(b.documentationScore()).append('\n');
		sb.append("- Original project work: ").append(b.originalityScore()).append('\n');
		sb.append("- Recent activity: ").append(b.activityScore()).append('\n');
		sb.append('\n');

		sb.append("Score explanations:\n");
		sb.append("- Overall: ").append(nullToEmpty(analysis.scoreExplanation())).append('\n');
		sb.append("- Project quality: ").append(nullToEmpty(analysis.projectQualityExplanation())).append('\n');
		sb.append("- Technology variety: ").append(nullToEmpty(analysis.technicalBreadthExplanation())).append('\n');
		sb.append("- Documentation: ").append(nullToEmpty(analysis.documentationExplanation())).append('\n');
		sb.append("- Original work: ").append(nullToEmpty(analysis.originalityExplanation())).append('\n');
		sb.append("- Activity: ").append(nullToEmpty(analysis.activityExplanation())).append('\n');
		sb.append('\n');

		sb.append("Profile strengths:\n");
		appendBullets(sb, analysis.strengths());
		sb.append("Profile weaknesses / gaps:\n");
		appendBullets(sb, analysis.weaknesses());
		sb.append("Technical highlights:\n");
		appendBullets(sb, analysis.technicalHighlights());
		sb.append("Growth areas:\n");
		appendBullets(sb, analysis.growthAreas());
		sb.append('\n');

		if (analysis.featuredRepo() != null) {
			RepoAnalysisDto featured = analysis.featuredRepo();
			sb.append("Strongest portfolio repo: ").append(featured.name());
			sb.append(" (score ").append(featured.repoScore()).append("/100");
			if (featured.language() != null && !featured.language().isBlank()) {
				sb.append(", ").append(featured.language());
			}
			sb.append(", stars ").append(featured.stars()).append(")\n");
			if (analysis.featuredRepoReason() != null) {
				sb.append("Why it is the strongest: ").append(analysis.featuredRepoReason()).append('\n');
			}
		}

		sb.append("\nAll reviewed repositories (name | score/100 | language | stars | flags):\n");
		for (RepoAnalysisDto repo : analysis.repos()) {
			sb.append("- ").append(repo.name())
					.append(" | ").append(repo.repoScore()).append("/100")
					.append(" | ").append(repo.language() == null ? "no language" : repo.language())
					.append(" | ").append(repo.stars()).append(" stars");
			if (repo.fork()) sb.append(" | fork");
			if (repo.likelyDemoRepo()) sb.append(" | demo/sample");
			if (repo.recentlyUpdated()) sb.append(" | recently active");
			if (!repo.hasDescription()) sb.append(" | no description");
			if (repo.readmeAnalysis() != null && !repo.readmeAnalysis().hasReadme()) sb.append(" | no README");
			if (repo.readmeAnalysis() != null && repo.readmeAnalysis().hasReadme() && !repo.readmeAnalysis().hasInstallationInstructions())
				sb.append(" | no install instructions");
			sb.append(" | ").append(repo.repoQualitySignal());
			sb.append('\n');
		}

		return sb.toString();
	}

	private static void appendBullets(StringBuilder sb, List<String> items) {
		if (items == null || items.isEmpty()) {
			sb.append("- (none)\n");
			return;
		}
		for (String item : items) {
			sb.append("- ").append(item).append('\n');
		}
	}

	private static String joinList(List<String> items) {
		if (items == null || items.isEmpty()) return "(none)";
		return items.stream().collect(Collectors.joining(", "));
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value.strip();
	}
}
