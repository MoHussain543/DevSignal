package com.devsignal.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Minimal GitHub GET /repos/{owner}/{repo}/readme JSON payload. */
public record GitHubReadmeApiDto(
		@JsonProperty("content") String content,
		@JsonProperty("encoding") String encoding
) {}
