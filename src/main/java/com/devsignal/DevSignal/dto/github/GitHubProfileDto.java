package com.devsignal.DevSignal.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubProfileDto(
		String login,
		String name,
		@JsonProperty("avatar_url") String avatarUrl,
		String bio,
		int followers,
		int following,
		@JsonProperty("public_repos") int publicRepos
) {}
