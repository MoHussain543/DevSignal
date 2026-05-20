package com.devsignal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public record SupabaseProperties(
		String url,
		String publishableKey
) {
	public boolean isConfigured() {
		return notBlank(url) && notBlank(publishableKey);
	}

	public String authUserEndpoint() {
		String base = url == null ? "" : url.strip();
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		return base + "/auth/v1/user";
	}

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}
}
