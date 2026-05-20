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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.devsignal.persistence.AnalysisRunRepository;

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
		AnalysisRunRepository analysisRunRepository = new InMemoryAnalysisRunRepository();
		AnalysisRunService analysisRunService =
				new AnalysisRunService(analysisRunRepository, new ObjectMapper());
		OpenAiProperties properties = new OpenAiProperties("", "gpt-5.4-mini", true);
		OpenAiReportService reportService =
				new OpenAiReportService(analysisService, analysisRunService, properties);

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

	private static class InMemoryAnalysisRunRepository implements AnalysisRunRepository {

		@Override
		public <S extends com.devsignal.persistence.AnalysisRun> S save(S entity) {
			return entity;
		}

		@Override
		public java.util.Optional<com.devsignal.persistence.AnalysisRun> findFirstByGithubUsernameNormalizedOrderByCreatedAtDesc(
				String githubUsernameNormalized) {
			return Optional.empty();
		}

		@Override
		public java.util.Optional<com.devsignal.persistence.AnalysisRun> findByRunKey(java.util.UUID runKey) {
			return Optional.empty();
		}

		@Override
		public java.util.List<com.devsignal.persistence.AnalysisRun> findByGithubUsernameNormalizedOrderByCreatedAtDesc(
				String githubUsernameNormalized) {
			return List.of();
		}

		@Override public java.util.List<com.devsignal.persistence.AnalysisRun> findAll() { throw unsupported(); }
		@Override public java.util.List<com.devsignal.persistence.AnalysisRun> findAllById(Iterable<java.util.UUID> ids) { throw unsupported(); }
		@Override public <S extends com.devsignal.persistence.AnalysisRun> java.util.List<S> saveAll(Iterable<S> entities) { throw unsupported(); }
		@Override public java.util.Optional<com.devsignal.persistence.AnalysisRun> findById(java.util.UUID uuid) { throw unsupported(); }
		@Override public boolean existsById(java.util.UUID uuid) { throw unsupported(); }
		@Override public long count() { throw unsupported(); }
		@Override public void deleteById(java.util.UUID uuid) { throw unsupported(); }
		@Override public void delete(com.devsignal.persistence.AnalysisRun entity) { throw unsupported(); }
		@Override public void deleteAllById(Iterable<? extends java.util.UUID> ids) { throw unsupported(); }
		@Override public void deleteAll(Iterable<? extends com.devsignal.persistence.AnalysisRun> entities) { throw unsupported(); }
		@Override public void deleteAll() { throw unsupported(); }
		@Override public void flush() { throw unsupported(); }
		@Override public <S extends com.devsignal.persistence.AnalysisRun> S saveAndFlush(S entity) { throw unsupported(); }
		@Override public <S extends com.devsignal.persistence.AnalysisRun> java.util.List<S> saveAllAndFlush(Iterable<S> entities) { throw unsupported(); }
		@Override public void deleteAllInBatch(Iterable<com.devsignal.persistence.AnalysisRun> entities) { throw unsupported(); }
		@Override public void deleteAllByIdInBatch(Iterable<java.util.UUID> ids) { throw unsupported(); }
		@Override public void deleteAllInBatch() { throw unsupported(); }
		@Override public com.devsignal.persistence.AnalysisRun getOne(java.util.UUID uuid) { throw unsupported(); }
		@Override public com.devsignal.persistence.AnalysisRun getById(java.util.UUID uuid) { throw unsupported(); }
		@Override public com.devsignal.persistence.AnalysisRun getReferenceById(java.util.UUID uuid) { throw unsupported(); }
		@Override public <S extends com.devsignal.persistence.AnalysisRun> java.util.Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw unsupported(); }
		@Override public <S extends com.devsignal.persistence.AnalysisRun> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example) { throw unsupported(); }
		@Override public <S extends com.devsignal.persistence.AnalysisRun> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw unsupported(); }
		@Override public <S extends com.devsignal.persistence.AnalysisRun> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw unsupported(); }
		@Override public <S extends com.devsignal.persistence.AnalysisRun> long count(org.springframework.data.domain.Example<S> example) { throw unsupported(); }
		@Override public <S extends com.devsignal.persistence.AnalysisRun> boolean exists(org.springframework.data.domain.Example<S> example) { throw unsupported(); }
		@Override public <S extends com.devsignal.persistence.AnalysisRun, R> R findBy(
				org.springframework.data.domain.Example<S> example,
				java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
			throw unsupported();
		}
		@Override public java.util.List<com.devsignal.persistence.AnalysisRun> findAll(org.springframework.data.domain.Sort sort) { throw unsupported(); }
		@Override public org.springframework.data.domain.Page<com.devsignal.persistence.AnalysisRun> findAll(org.springframework.data.domain.Pageable pageable) { throw unsupported(); }

		private UnsupportedOperationException unsupported() {
			return new UnsupportedOperationException("Not needed for this test");
		}
	}
}
