package com.devsignal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.devsignal.client.github.GitHubClient;
import com.devsignal.config.OpenAiProperties;
import com.devsignal.dto.ai.AiReportResponseDto;
import com.devsignal.dto.github.GitHubProfileDto;
import com.devsignal.dto.github.GitHubRepoDto;

class OpenAiReportServiceTest {

	@Test
	void buildReportReturnsAnalysisWhenOpenAiNotConfigured() {
		Instant now = Instant.now();
		GitHubProfileDto profile = new GitHubProfileDto("octocat", "Octocat", null, null, 8, 0, 8);
		List<GitHubRepoDto> repos = List.of(
				new GitHubRepoDto(
						"Hello-World",
						"My first repository on GitHub!",
						"Ruby",
						500,
						200,
						now.minus(400, ChronoUnit.DAYS),
						now.minus(1, ChronoUnit.DAYS),
						false));

		GitHubAnalysisService analysisService =
				new GitHubAnalysisService(new StubGitHubClient(profile, repos));
		OpenAiProperties properties = new OpenAiProperties("", "gpt-5.4-mini", true);
		OpenAiReportService reportService = new OpenAiReportService(analysisService, properties);

		AiReportResponseDto response = reportService.buildReport("octocat");

		assertNotNull(response.analysis());
		assertEquals("octocat", response.analysis().username());
		assertFalse(response.aiSummary().available());
		assertNotNull(response.aiSummary().unavailableReason());
		assertTrue(response.aiSummary().unavailableReason().contains("OPENAI_API_KEY"));
	}

	private static class StubGitHubClient extends GitHubClient {

		private final GitHubProfileDto profile;
		private final List<GitHubRepoDto> repos;

		private StubGitHubClient(GitHubProfileDto profile, List<GitHubRepoDto> repos) {
			super("");
			this.profile = profile;
			this.repos = repos;
		}

		@Override
		public GitHubProfileDto getUserProfile(String username) {
			return profile;
		}

		@Override
		public List<GitHubRepoDto> getUserRepos(String username) {
			return repos;
		}

		@Override
		public Optional<String> fetchRepositoryReadmeContent(String owner, String repoName) {
			return Optional.empty();
		}
	}
}
