package com.devsignal.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
		List<String> allowedOriginPatterns,
		List<String> allowedMethods,
		List<String> allowedHeaders
) {
	public List<String> allowedOriginPatterns() {
		return normalizeOrDefault(allowedOriginPatterns, List.of("http://localhost:5173", "http://localhost:3000"));
	}

	public List<String> allowedMethods() {
		return normalizeOrDefault(allowedMethods, List.of("GET", "POST", "OPTIONS"));
	}

	public List<String> allowedHeaders() {
		return normalizeOrDefault(allowedHeaders, List.of("Authorization", "Content-Type"));
	}

	private static List<String> normalizeOrDefault(List<String> values, List<String> defaults) {
		if (values == null) {
			return defaults;
		}
		List<String> normalized = values.stream()
				.filter(value -> value != null && !value.isBlank())
				.map(String::trim)
				.toList();
		return normalized.isEmpty() ? defaults : normalized;
	}
}
