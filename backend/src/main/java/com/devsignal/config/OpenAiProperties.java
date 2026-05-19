package com.devsignal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
		String apiKey,
		String model,
		boolean enabled
) {
	public String resolvedModel() {
		return model == null || model.isBlank() ? "gpt-5.4-mini" : model.strip();
	}

	public boolean isConfigured() {
		return apiKey != null && !apiKey.isBlank();
	}
}
