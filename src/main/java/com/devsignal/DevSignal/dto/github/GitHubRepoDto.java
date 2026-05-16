package com.devsignal.DevSignal.dto.github;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepoDto(
		String name,
		String description,
		String language,
		@JsonProperty("stargazers_count") int stars,
		@JsonProperty("forks_count") int forks,
		@JsonProperty("created_at") Instant createdAt,
		@JsonProperty("updated_at") Instant updatedAt,
		boolean fork
) {}
