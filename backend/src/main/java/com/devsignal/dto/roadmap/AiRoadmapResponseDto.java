package com.devsignal.dto.roadmap;

import com.devsignal.dto.analysis.AnalysisResponseDto;

/** Full roadmap payload: deterministic analysis plus optional AI roadmap. */
public record AiRoadmapResponseDto(
		AnalysisResponseDto analysis,
		AiRoadmapDto roadmap
) {}
