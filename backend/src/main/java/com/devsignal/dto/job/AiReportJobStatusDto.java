package com.devsignal.dto.job;

import java.util.UUID;

import com.devsignal.dto.ai.AiReportResponseDto;
import com.devsignal.persistence.AnalysisRunStatus;

public record AiReportJobStatusDto(
		UUID runKey,
		String status,
		String errorMessage,
		AiReportResponseDto result
) {
	public static AiReportJobStatusDto from(
			UUID runKey,
			AnalysisRunStatus status,
			String errorMessage,
			AiReportResponseDto result) {
		return new AiReportJobStatusDto(runKey, status.dbValue(), errorMessage, result);
	}
}
