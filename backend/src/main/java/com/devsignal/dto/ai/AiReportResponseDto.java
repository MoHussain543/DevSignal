package com.devsignal.dto.ai;

import com.devsignal.dto.analysis.AnalysisResponseDto;

/** Full report payload: deterministic analysis plus optional AI narrative. */
public record AiReportResponseDto(
		AnalysisResponseDto analysis,
		AiReportDto aiSummary
) {}
