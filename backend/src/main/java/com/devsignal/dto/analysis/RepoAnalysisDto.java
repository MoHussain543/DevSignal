package com.devsignal.dto.analysis;

import java.util.List;

public record RepoAnalysisDto(
		String name,
		String description,
		String language,
		int stars,
		int forks,
		boolean hasDescription,
		boolean recentlyUpdated,
		boolean fork,
		boolean originalRepo,
		boolean hasLanguage,
		boolean hasStrongDescription,
		boolean likelyDemoRepo,
		ReadmeAnalysisDto readmeAnalysis,
		String repoQualitySignal,
		/** Signals without README-derived points (fed into project-quality pillar — README scored separately). */
		int repoMetadataScore,
		int repoScore,
		List<String> repoSignals
) {}
