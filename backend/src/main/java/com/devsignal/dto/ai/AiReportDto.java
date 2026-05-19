package com.devsignal.dto.ai;

import java.util.List;

/**
 * AI-generated narrative layered on top of {@link com.devsignal.dto.analysis.AnalysisResponseDto}.
 * Scores and facts always come from the backend analysis; this is narration only.
 */
public record AiReportDto(
		boolean available,
		String overallSummary,
		String overallRead,
		String hiringSignal,
		String mainGap,
		String bestSignal,
		String howProfileReadsPattern,
		String howProfileReadsDrivers,
		String howProfileReadsHelps,
		String howProfileReadsHoldsBack,
		String hiringImpression,
		String whatStandsOut,
		String whatWeakens,
		List<String> positiveSignals,
		List<String> warningSignals,
		List<String> missingSignals,
		List<AiPriorityItemDto> topPriorities,
		String biggestUnlock,
		String unavailableReason
) {
	private static final List<String> EMPTY_SIGNALS = List.of();
	private static final List<AiPriorityItemDto> EMPTY_PRIORITIES = List.of();

	public static AiReportDto available(
			String overallSummary,
			String overallRead,
			String hiringSignal,
			String mainGap,
			String bestSignal,
			String howProfileReadsPattern,
			String howProfileReadsDrivers,
			String howProfileReadsHelps,
			String howProfileReadsHoldsBack,
			String hiringImpression,
			String whatStandsOut,
			String whatWeakens,
			List<String> positiveSignals,
			List<String> warningSignals,
			List<String> missingSignals,
			List<AiPriorityItemDto> topPriorities,
			String biggestUnlock) {
		return new AiReportDto(
				true,
				overallSummary,
				overallRead,
				hiringSignal,
				mainGap,
				bestSignal,
				howProfileReadsPattern,
				howProfileReadsDrivers,
				howProfileReadsHelps,
				howProfileReadsHoldsBack,
				hiringImpression,
				whatStandsOut,
				whatWeakens,
				safeList(positiveSignals),
				safeList(warningSignals),
				safeList(missingSignals),
				safePriorities(topPriorities),
				biggestUnlock,
				null);
	}

	public static AiReportDto unavailable(String reason) {
		return new AiReportDto(
				false,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				EMPTY_SIGNALS,
				EMPTY_SIGNALS,
				EMPTY_SIGNALS,
				EMPTY_PRIORITIES,
				null,
				reason);
	}

	private static List<String> safeList(List<String> items) {
		return items == null ? EMPTY_SIGNALS : List.copyOf(items);
	}

	private static List<AiPriorityItemDto> safePriorities(List<AiPriorityItemDto> items) {
		return items == null ? EMPTY_PRIORITIES : List.copyOf(items);
	}
}
