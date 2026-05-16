package com.devsignal.dto.analysis;

public record ScoreBreakdownDto(
		int projectQualityScore,
		/** Distinct primary languages across qualifying portfolio repos (log-scaled). */
		int technicalBreadthScore,
		int activityScore,
		int documentationScore,
		int originalityScore
) {}
