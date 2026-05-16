package com.devsignal.client.github;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.devsignal.dto.github.GitHubProfileDto;
import com.devsignal.dto.github.GitHubRepoDto;

@Component
public class GitHubClient {

	private static final String BASE_URL = "https://api.github.com";
	private static final int RESPONSE_BODY_PREVIEW_MAX_CHARS = 512;

	private static final ParameterizedTypeReference<List<GitHubRepoDto>> REPO_LIST_TYPE =
			new ParameterizedTypeReference<List<GitHubRepoDto>>() {};

	private final RestClient restClient;

	public GitHubClient(@Value("${github.token:}") String token) {
		RestClient.Builder builder = RestClient.builder()
				.baseUrl(BASE_URL)
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader(HttpHeaders.USER_AGENT, "DevSignal");
		String trimmedToken = token == null ? "" : token.trim();
		if (!trimmedToken.isEmpty()) {
			builder = builder.defaultHeader(HttpHeaders.AUTHORIZATION, "token " + trimmedToken);
		}
		this.restClient = builder.build();
	}

	public GitHubProfileDto getUserProfile(String username) {
		try {
			return restClient.get()
					.uri("/users/{username}", username)
					.retrieve()
					.body(GitHubProfileDto.class);
		}
		catch (RestClientResponseException ex) {
			throw apiException(username, "user profile", ex);
		}
		catch (RestClientException ex) {
			throw networkException(username, "user profile", ex);
		}
	}

	public List<GitHubRepoDto> getUserRepos(String username) {
		try {
			List<GitHubRepoDto> repos = restClient.get()
					.uri("/users/{username}/repos?per_page=100", username)
					.retrieve()
					.body(REPO_LIST_TYPE);
			return repos == null ? List.of() : repos;
		}
		catch (RestClientResponseException ex) {
			throw apiException(username, "repositories list", ex);
		}
		catch (RestClientException ex) {
			throw networkException(username, "repositories list", ex);
		}
	}

	private GitHubApiException apiException(String username, String operation, RestClientResponseException ex) {
		String bodySnippet = abbreviateBody(ex.getResponseBodyAsString(StandardCharsets.UTF_8));
		String message = "GitHub API error while fetching %s for '%s': HTTP %s %s — %s"
				.formatted(operation, username, ex.getStatusCode().value(),
						ex.getStatusText(), bodySnippet);
		return new GitHubApiException(message, ex.getStatusCode(), ex);
	}

	private GitHubApiException networkException(String username, String operation, RestClientException ex) {
		String message = "GitHub request failed while fetching %s for '%s': %s"
				.formatted(operation, username, ex.getMessage());
		return new GitHubApiException(message, null, ex);
	}

	private static String abbreviateBody(String body) {
		if (body == null || body.isBlank()) {
			return "(no response body)";
		}
		String trimmed = body.trim().replaceAll("\\s+", " ");
		if (trimmed.length() <= RESPONSE_BODY_PREVIEW_MAX_CHARS) {
			return trimmed;
		}
		return trimmed.substring(0, RESPONSE_BODY_PREVIEW_MAX_CHARS) + "…";
	}

	/**
	 * Thrown when a GitHub HTTP call fails or the response cannot be processed as expected.
	 * Prefer {@link #getStatusCode()} for mapping to REST status in higher layers when present.
	 */
	public static final class GitHubApiException extends RuntimeException {

		/** Present for HTTP error responses; {@code null} for transport-level failures. */
		private final HttpStatusCode statusCode;

		public GitHubApiException(String message, HttpStatusCode statusCode, Throwable cause) {
			super(message, cause);
			this.statusCode = statusCode;
		}

		/** @return response status when GitHub returned an HTTP error; {@code null} if the request did not get a response (e.g. timeout). */
		public HttpStatusCode getStatusCode() {
			return statusCode;
		}
	}
}
