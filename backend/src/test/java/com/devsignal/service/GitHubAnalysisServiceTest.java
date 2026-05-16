package com.devsignal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.devsignal.client.github.GitHubClient;
import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.dto.analysis.RepoAnalysisDto;
import com.devsignal.dto.github.GitHubProfileDto;
import com.devsignal.dto.github.GitHubRepoDto;

class GitHubAnalysisServiceTest {

	@Test
	void analyzeUsesPortfolioNamesForPortfolioOnlyMetrics() {
		Instant now = Instant.now();
		GitHubProfileDto profile = new GitHubProfileDto(
				"octocat",
				"The Octocat",
				"https://example.com/avatar.png",
				null,
				100,
				0,
				8
		);
		List<GitHubRepoDto> repos = List.of(
				new GitHubRepoDto(
						"portfolio-site",
						"Production portfolio website with custom frontend and deployment setup.",
						"TypeScript",
						12,
						3,
						now.minus(90, ChronoUnit.DAYS),
						now.minus(2, ChronoUnit.DAYS),
						false
				),
				new GitHubRepoDto(
						"Hello-World",
						"My first repository on GitHub!",
						"Ruby",
						500,
						200,
						now.minus(400, ChronoUnit.DAYS),
						now.minus(1, ChronoUnit.DAYS),
						false
				),
				new GitHubRepoDto(
						"forked-tooling",
						"Useful fork for experiments.",
						"Go",
						50,
						20,
						now.minus(200, ChronoUnit.DAYS),
						now.minus(3, ChronoUnit.DAYS),
						true
				)
		);
		GitHubAnalysisService service = new GitHubAnalysisService(new StubGitHubClient(profile, repos));

		AnalysisResponseDto response = service.analyze("octocat");

		assertEquals(3, response.analyzedRepoCount());
		assertEquals(2, response.originalRepoCount());
		assertEquals(1, response.portfolioRepoCount());
		assertEquals(12, response.portfolioTotalStars());
		assertEquals(3, response.portfolioTotalForks());
		assertEquals(52, response.portfolioAverageRepoScore());
		assertEquals(List.of("TypeScript"), response.portfolioTopLanguages());

		assertEquals("portfolio-site", response.featuredRepo().name());
		assertTrue(response.featuredRepoReason() != null && !response.featuredRepoReason().isBlank());

		List<String> displayOrder =
				response.repos().stream().map(RepoAnalysisDto::name).toList();
		assertEquals(List.of("portfolio-site", "Hello-World", "forked-tooling"), displayOrder,
				"highest-value repos (qualifying portfolio first, then score, forks last)");
	}

	@Test
	void analyzeDoesNotTreatGeneralTestingReposAsDemoRepos() {
		Instant now = Instant.now();
		GitHubProfileDto profile = new GitHubProfileDto(
				"devsignal",
				"Dev Signal",
				"https://example.com/avatar.png",
				null,
				10,
				0,
				2
		);
		List<GitHubRepoDto> repos = List.of(
				new GitHubRepoDto(
						"integration-tests",
						"End-to-end test harness for validating production workflows.",
						"Java",
						4,
						1,
						now.minus(120, ChronoUnit.DAYS),
						now.minus(1, ChronoUnit.DAYS),
						false
				),
				new GitHubRepoDto(
						"test-repo1",
						null,
						null,
						0,
						0,
						now.minus(120, ChronoUnit.DAYS),
						now.minus(1, ChronoUnit.DAYS),
						false
				)
		);
		GitHubAnalysisService service = new GitHubAnalysisService(new StubGitHubClient(profile, repos));

		AnalysisResponseDto response = service.analyze("devsignal");
		RepoAnalysisDto integrationTests = response.repos().stream()
				.filter(repo -> "integration-tests".equals(repo.name()))
				.findFirst()
				.orElseThrow();
		RepoAnalysisDto testRepo = response.repos().stream()
				.filter(repo -> "test-repo1".equals(repo.name()))
				.findFirst()
				.orElseThrow();

		assertFalse(integrationTests.likelyDemoRepo());
		assertTrue(testRepo.likelyDemoRepo());
		assertEquals(1, response.portfolioRepoCount());
	}

	@Test
	void technicalBreadthScoreUsesLogCurveNotLinear() {
		// 4 portfolio repos with 4 distinct languages.
		// Log curve: round(100 * log(5) / log(9)) ≈ 73
		// Old linear formula: min(100, 4*34) = 100 — would saturate at 3 langs
		Instant now = Instant.now();
		GitHubProfileDto profile = new GitHubProfileDto("multi-lang", "Multi Lang Dev", null, null, 10, 0, 4);
		List<GitHubRepoDto> repos = List.of(
				new GitHubRepoDto("java-app", "Production Java application with Spring Boot deployment setup.", "Java", 5, 1,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), false),
				new GitHubRepoDto("python-tool", "Production Python data processing tool for analytics.", "Python", 5, 1,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), false),
				new GitHubRepoDto("react-frontend", "Production React frontend with deployment and CI setup.", "JavaScript", 5, 1,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), false),
				new GitHubRepoDto("go-service", "Production Go microservice with REST API endpoints.", "Go", 5, 1,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), false)
		);
		GitHubAnalysisService service = new GitHubAnalysisService(new StubGitHubClient(profile, repos));

		AnalysisResponseDto response = service.analyze("multi-lang");

		assertEquals(4, response.portfolioTopLanguages().size());
		assertEquals(73, response.scoreBreakdown().technicalBreadthScore(),
				"log curve: 4 languages → ~73, not 100 (old linear formula saturated at 3 langs)");
	}

	@Test
	void repoScoreUsesSteppedStarBonus() {
		// Baseline hygiene 45 + star bucket; readme stub empty (+0 README blend).
		// 45 + stars: 1 → +3 ⇒ 48; 12 (+7 tier) ⇒ 52; 50 (+11 tier) ⇒ 56
		Instant now = Instant.now();
		GitHubProfileDto profile = new GitHubProfileDto("star-test", "Star Test", null, null, 3, 0, 3);
		List<GitHubRepoDto> repos = List.of(
				new GitHubRepoDto("one-star", "Production project with a clear deployment and setup guide.", "Java", 1, 0,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), false),
				new GitHubRepoDto("twelve-stars", "Production project with a clear deployment and setup guide.", "Java", 12, 0,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), false),
				new GitHubRepoDto("fifty-stars", "Production project with a clear deployment and setup guide.", "Java", 50, 0,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), false)
		);
		GitHubAnalysisService service = new GitHubAnalysisService(new StubGitHubClient(profile, repos));

		AnalysisResponseDto response = service.analyze("star-test");

		java.util.Map<String, Integer> scoreByName = response.repos().stream()
				.collect(java.util.stream.Collectors.toMap(
						com.devsignal.dto.analysis.RepoAnalysisDto::name,
						com.devsignal.dto.analysis.RepoAnalysisDto::repoScore));
		assertEquals(48, scoreByName.get("one-star"),     "45 + 3 star bonus");
		assertEquals(52, scoreByName.get("twelve-stars"), "45 + 7 star bucket");
		assertEquals(56, scoreByName.get("fifty-stars"),  "45 + 11 star bucket");
	}

	@Test
	void originalityScoreUsesAnalyzedSampleDenominator() {
		// 3 analyzed repos: 1 portfolio (original, non-demo), 1 demo (original), 1 fork.
		// analyzedOriginalRepoCount = 2; portfolioRepoCount = 1.
		// Expected originality = clamp(round(1 * 75.0 / 2 + min(25, 1*8))) = clamp(round(37.5 + 8)) = clamp(46) = 46
		Instant now = Instant.now();
		GitHubProfileDto profile = new GitHubProfileDto("orig-test", "Orig Test", null, null, 3, 0, 3);
		List<GitHubRepoDto> repos = List.of(
				new GitHubRepoDto("real-app", "Production application with deployment and setup documentation.", "Java", 5, 1,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), false),
				new GitHubRepoDto("hello-world", "My first repository on GitHub!", "Java", 0, 0,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), false),
				new GitHubRepoDto("fork-of-lib", "Forked library for experiments.", "Java", 0, 0,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), true)
		);
		GitHubAnalysisService service = new GitHubAnalysisService(new StubGitHubClient(profile, repos));

		AnalysisResponseDto response = service.analyze("orig-test");

		assertEquals(1, response.portfolioRepoCount());
		assertEquals(46, response.scoreBreakdown().originalityScore(),
				"denominator should be analyzedOriginalRepoCount (2), not totalFetchedOriginalRepoCount");
	}

	@Test
	void projectQualityUsesSmoothedRepoCountBonus() {
		Instant now = Instant.now();
		GitHubProfileDto profile = new GitHubProfileDto("pq-bonus", "PQ Bonus", null, null, 5, 0, 5);
		GitHubRepoDto template = new GitHubRepoDto(
				"app",
				"Production application with deployment and setup documentation.",
				"Java",
				5,
				1,
				now.minus(30, ChronoUnit.DAYS),
				now.minus(1, ChronoUnit.DAYS),
				false);
		List<GitHubRepoDto> oneRepo = List.of(cloneRepo(template, "app-a"));
		List<GitHubRepoDto> threeRepos = List.of(
				cloneRepo(template, "app-a"),
				cloneRepo(template, "app-b"),
				cloneRepo(template, "app-c"));

		GitHubAnalysisService service =
				new GitHubAnalysisService(new StubGitHubClient(profile, oneRepo));
		int oneAdjusted =
				service.analyze("pq-bonus").scoreBreakdown().projectQualityScore();

		service = new GitHubAnalysisService(new StubGitHubClient(profile, threeRepos));
		int threeAdjusted = service.analyze("pq-bonus").scoreBreakdown().projectQualityScore();

		assertTrue(threeAdjusted > oneAdjusted, "stepped bonus should lift multi-repo portfolios without oversized 1→3 jumps");
	}

	private static GitHubRepoDto cloneRepo(GitHubRepoDto base, String name) {
		return new GitHubRepoDto(
				name,
				base.description(),
				base.language(),
				base.stars(),
				base.forks(),
				base.createdAt(),
				base.updatedAt(),
				base.fork());
	}

	@Test
	void featuredRepoPicksHigherScoreAmongPortfolioRepos() {
		Instant now = Instant.now();
		GitHubProfileDto profile =
				new GitHubProfileDto("featured-pick", "Featured", null, null, 4, 0, 4);
		List<GitHubRepoDto> repos = List.of(
				new GitHubRepoDto(
						"lower-app",
						"Production project with deployment and setup documentation.",
						"Go",
						2,
						0,
						now.minus(30, ChronoUnit.DAYS),
						now.minus(2, ChronoUnit.DAYS),
						false),
				new GitHubRepoDto(
						"higher-app",
						"Production project with deployment and setup documentation and operational tooling.",
						"Go",
						40,
						2,
						now.minus(30, ChronoUnit.DAYS),
						now.minus(1, ChronoUnit.DAYS),
						false));
		GitHubAnalysisService service = new GitHubAnalysisService(new StubGitHubClient(profile, repos));
		AnalysisResponseDto response = service.analyze("featured-pick");

		assertEquals("higher-app", response.featuredRepo().name());
	}

	@Test
	void readmePresenceStartsAtTenForMinimalReadme() {
		Instant now = Instant.now();
		GitHubProfileDto profile =
				new GitHubProfileDto("readme-base", "Readme Base", null, null, 1, 0, 1);
		List<GitHubRepoDto> repos = List.of(
				new GitHubRepoDto(
						"solo-app",
						"Production project with deployment and setup documentation.",
						"Java",
						4,
						1,
						now.minus(30, ChronoUnit.DAYS),
						now.minus(1, ChronoUnit.DAYS),
						false));
		GitHubClient client = new StubGitHubClient(profile, repos) {
			@Override
			public Optional<String> fetchRepositoryReadmeContent(String owner, String repoName) {
				if ("solo-app".equals(repoName)) {
					return Optional.of("# hi");
				}
				return Optional.empty();
			}
		};
		AnalysisResponseDto response = new GitHubAnalysisService(client).analyze("readme-base");
		RepoAnalysisDto repo = response.repos().stream().filter(r -> "solo-app".equals(r.name())).findFirst().orElseThrow();
		assertEquals(10, repo.readmeAnalysis().documentationScore());
	}

	@Test
	void readmeDocumentationRubricCanReachOneHundred() {
		Instant now = Instant.now();
		GitHubProfileDto profile =
				new GitHubProfileDto("readme-max", "Readme Max", null, null, 1, 0, 1);
		List<GitHubRepoDto> repos = List.of(
				new GitHubRepoDto(
						"doc-rich",
						"Production project with deployment and setup documentation.",
						"Java",
						4,
						1,
						now.minus(30, ChronoUnit.DAYS),
						now.minus(1, ChronoUnit.DAYS),
						false));

		StringBuilder body = new StringBuilder();
		body.append("# Title\n\n## Installation\nSetup and install guides here.\n\n");
		body.append("## Usage\nHow to run and example commands.\n\n");
		body.append("## Features\nFunctionality overview and what it does.\n\n");
		body.append("## Tech stack\nBuilt with React PostgreSQL MongoDB docker.\n\n");
		body.append("![screenshot demo](img.png)\n\n");
		while (body.length() < 1005) {
			body.append("Narrative section with more wording for length.\n\n");
		}

		GitHubClient client = new StubGitHubClient(profile, repos) {
			@Override
			public Optional<String> fetchRepositoryReadmeContent(String owner, String repoName) {
				if ("doc-rich".equals(repoName)) {
					return Optional.of(body.toString());
				}
				return Optional.empty();
			}
		};
		AnalysisResponseDto response = new GitHubAnalysisService(client).analyze("readme-max");
		int doc = response.repos().stream()
				.filter(r -> "doc-rich".equals(r.name())).findFirst().orElseThrow()
				.readmeAnalysis().documentationScore();
		assertEquals(100, doc);
	}

	@Test
	void featuredRepoUsesHigherStarsAfterRepoScoreTie() {
		Instant now = Instant.now();
		GitHubProfileDto profile =
				new GitHubProfileDto("star-tie", "Star Tie", null, null, 2, 0, 2);
		String desc =
				"Production project with deployment and setup documentation and operational tooling.";
		List<GitHubRepoDto> repos = List.of(
				new GitHubRepoDto(
						"low-star-app",
						desc,
						"Go",
						31,
						0,
						now.minus(30, ChronoUnit.DAYS),
						now.minus(3, ChronoUnit.DAYS),
						false),
				new GitHubRepoDto(
						"high-star-app",
						desc,
						"Go",
						41,
						0,
						now.minus(30, ChronoUnit.DAYS),
						now.minus(1, ChronoUnit.DAYS),
						false));

		GitHubAnalysisService service = new GitHubAnalysisService(new StubGitHubClient(profile, repos));
		AnalysisResponseDto response = service.analyze("star-tie");

		int scoreLow =
				response.repos().stream().filter(r -> "low-star-app".equals(r.name())).findFirst().orElseThrow()
						.repoScore();
		int scoreHigh =
				response.repos().stream().filter(r -> "high-star-app".equals(r.name())).findFirst().orElseThrow()
						.repoScore();
		assertEquals(scoreLow, scoreHigh, "same star bucket and signals → identical repoScores");
		assertEquals("high-star-app", response.featuredRepo().name());
	}

	@Test
	void technicalHighlightsUseAnalyzedSampleWording() {
		Instant now = Instant.now();
		GitHubProfileDto profile = new GitHubProfileDto("wording", "Wording", null, null, 5, 0, 5);
		List<GitHubRepoDto> repos = List.of(
				new GitHubRepoDto("a", "Production project one with deployment setup.", "Java", 3, 0,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), false),
				new GitHubRepoDto("b", "Production project two with deployment setup.", "Python", 2, 0,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), false),
				new GitHubRepoDto("c", "Production project three with deployment setup.", "Go", 1, 0,
						now.minus(30, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), false));
		AnalysisResponseDto response =
				new GitHubAnalysisService(new StubGitHubClient(profile, repos)).analyze("wording");
		assertTrue(response.technicalHighlights().stream().anyMatch(s -> s.contains("analyzed")));
		assertTrue(response.strengths().stream().anyMatch(s -> s.contains("analyzed")));
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
