package com.devsignal.dto.analysis;

public record ReadmeAnalysisDto(
		boolean hasReadme,
		int readmeLength,
		boolean hasInstallationInstructions,
		boolean hasUsageInstructions,
		boolean hasTechStackMention,
		boolean hasFeatureSection,
		boolean hasScreenshots,
		int documentationScore
) {}
