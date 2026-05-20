package com.devsignal.dto.job;

import java.util.UUID;

import com.devsignal.dto.roadmap.AiRoadmapResponseDto;
import com.devsignal.persistence.AnalysisRunStatus;

public record AiRoadmapJobStatusDto(
		UUID runKey,
		String status,
		String errorMessage,
		AiRoadmapResponseDto result
) {
	public static AiRoadmapJobStatusDto from(
			UUID runKey,
			AnalysisRunStatus status,
			String errorMessage,
			AiRoadmapResponseDto result) {
		return new AiRoadmapJobStatusDto(runKey, status.dbValue(), errorMessage, result);
	}
}
