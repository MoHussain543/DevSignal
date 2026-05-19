package com.devsignal.dto.roadmap;

import java.util.List;

/**
 * AI-generated, portfolio-focused roadmap layered on top of the deterministic analysis.
 * Every field is evidence-based: the AI derives recommendations from actual GitHub signals.
 */
public record AiRoadmapDto(
		boolean available,
		String roadmapSummary,
		List<QuickWinDto> quickWins,
		List<SkillDto> skillsToLearnNext,
		List<ProjectIdeaDto> nextProjectDirection,
		String highestImpactChange,
		String monthOnePlan,
		String monthTwoPlan,
		String monthThreePlan,
		String expectedOutcome,
		List<String> doList,
		List<String> avoidList,
		String unavailableReason
) {

	public static AiRoadmapDto available(
			String roadmapSummary,
			List<QuickWinDto> quickWins,
			List<SkillDto> skillsToLearnNext,
			List<ProjectIdeaDto> nextProjectDirection,
			String highestImpactChange,
			String monthOnePlan,
			String monthTwoPlan,
			String monthThreePlan,
			String expectedOutcome,
			List<String> doList,
			List<String> avoidList) {
		return new AiRoadmapDto(
				true,
				roadmapSummary,
				quickWins,
				skillsToLearnNext,
				nextProjectDirection,
				highestImpactChange,
				monthOnePlan,
				monthTwoPlan,
				monthThreePlan,
				expectedOutcome,
				doList,
				avoidList,
				null);
	}

	public static AiRoadmapDto unavailable(String reason) {
		return new AiRoadmapDto(false, null, null, null, null, null, null, null, null, null, null, null, reason);
	}
}
