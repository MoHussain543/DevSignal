package com.devsignal.exception;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.devsignal.client.github.GitHubClient.GitHubApiException;

@RestControllerAdvice
public class GitHubApiExceptionHandler {

	@ExceptionHandler(GitHubApiException.class)
	public ProblemDetail handleGitHubApi(GitHubApiException ex) {
		HttpStatus status = resolveHttpStatus(ex.getStatusCode());
		return ProblemDetail.forStatusAndDetail(status, ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
		String detail = Objects.requireNonNullElse(ex.getMessage(), "Invalid request parameters");
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
	}

	/**
	 * Maps GitHub HTTP status for clients; transport failures ({@code null} status) use {@link HttpStatus#BAD_GATEWAY}.
	 */
	private static HttpStatus resolveHttpStatus(HttpStatusCode githubStatus) {
		if (githubStatus == null) {
			return HttpStatus.BAD_GATEWAY;
		}
		HttpStatus resolved = HttpStatus.resolve(githubStatus.value());
		return resolved != null ? resolved : HttpStatus.BAD_GATEWAY;
	}
}
