package com.devsignal.dto.github;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepoDto(
		String name,
		String description,
		String language,
		@JsonProperty("stargazers_count") int stars,
		@JsonProperty("forks_count") int forks,
		@JsonProperty("created_at") Instant createdAt,
		@JsonProperty("updated_at") Instant updatedAt,
		boolean fork,
		@JsonProperty("topics") List<String> topics
) {

	public GitHubRepoDto {
		topics = topics == null ? List.of() : topics;
	}

	/** Tests and simple call-sites; GitHub omits topics when absent. */
	public GitHubRepoDto(
			String name,
			String description,
			String language,
			int stars,
			int forks,
			Instant createdAt,
			Instant updatedAt,
			boolean fork) {
		this(name, description, language, stars, forks, createdAt, updatedAt, fork, List.of());
	}
}
