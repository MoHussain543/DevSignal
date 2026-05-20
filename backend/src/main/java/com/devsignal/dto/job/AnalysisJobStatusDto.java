package com.devsignal.dto.job;

import java.util.UUID;

import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.persistence.AnalysisRunStatus;

public record AnalysisJobStatusDto(
		UUID runKey,
		String status,
		String errorMessage,
		AnalysisResponseDto result
) {
	public static AnalysisJobStatusDto from(
			UUID runKey,
			AnalysisRunStatus status,
			String errorMessage,
			AnalysisResponseDto result) {
		return new AnalysisJobStatusDto(runKey, status.dbValue(), errorMessage, result);
	}
}
