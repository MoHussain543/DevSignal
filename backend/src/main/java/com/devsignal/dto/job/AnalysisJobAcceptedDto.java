package com.devsignal.dto.job;

import java.util.UUID;

import com.devsignal.persistence.AnalysisRunStatus;

public record AnalysisJobAcceptedDto(
		UUID runKey,
		String status
) {
	public static AnalysisJobAcceptedDto from(UUID runKey, AnalysisRunStatus status) {
		return new AnalysisJobAcceptedDto(runKey, status.dbValue());
	}
}
