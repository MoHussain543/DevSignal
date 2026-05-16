package com.devsignal.dto.analysis;

import java.util.List;

public record AnalysisResponseDto(
		String username,
		String name,
		String avatarUrl,
		String bio,
		int publicRepos,
		int followers,
		long totalStars,
		long totalForks,
		List<String> topLanguages,
		List<String> strengths,
		List<String> weaknesses,
		int score
) {}
