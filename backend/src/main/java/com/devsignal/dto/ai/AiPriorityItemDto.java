package com.devsignal.dto.ai;

/** One ranked near-term action in the AI report. */
public record AiPriorityItemDto(
		String action,
		String whyItMatters,
		String visibleImprovement
) {
}
