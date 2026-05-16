package com.devsignal.DevSignal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.devsignal.DevSignal.client.github.GitHubClient.GitHubApiException;

@RestControllerAdvice
public class GitHubApiExceptionHandler {

	@ExceptionHandler(GitHubApiException.class)
	public ProblemDetail handleGitHubApi(GitHubApiException ex) {
		HttpStatus status = resolveHttpStatus(ex.getStatusCode());
		return ProblemDetail.forStatusAndDetail(status, ex.getMessage());
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
