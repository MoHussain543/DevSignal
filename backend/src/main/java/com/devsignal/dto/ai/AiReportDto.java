package com.devsignal.dto.ai;

/**
 * AI-generated narrative layered on top of {@link com.devsignal.dto.analysis.AnalysisResponseDto}.
 * Scores and facts always come from the backend analysis; this is narration only.
 */
public record AiReportDto(
		boolean available,
		String overallSummary,
		String hiringImpression,
		String whatStandsOut,
		String whatWeakens,
		String improveFirst,
		String biggestUnlock,
		String unavailableReason
) {
	public static AiReportDto available(
			String overallSummary,
			String hiringImpression,
			String whatStandsOut,
			String whatWeakens,
			String improveFirst,
			String biggestUnlock) {
		return new AiReportDto(
				true,
				overallSummary,
				hiringImpression,
				whatStandsOut,
				whatWeakens,
				improveFirst,
				biggestUnlock,
				null);
	}

	public static AiReportDto unavailable(String reason) {
		return new AiReportDto(false, null, null, null, null, null, null, reason);
	}
}
