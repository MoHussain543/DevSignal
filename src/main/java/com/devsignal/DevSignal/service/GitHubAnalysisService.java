package com.devsignal.DevSignal.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.devsignal.DevSignal.client.github.GitHubClient;
import com.devsignal.DevSignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.DevSignal.dto.github.GitHubProfileDto;
import com.devsignal.DevSignal.dto.github.GitHubRepoDto;

@Service
public class GitHubAnalysisService {

	private static final int SCORE_BONUS_EACH = 20;
	private static final int MIN_REPOS_ORIGINAL_MULTIPLICITY_STRENGTH = 5;
	private static final int MIN_DISTINCT_LANGUAGES_STRENGTH = 3;

	private final GitHubClient gitHubClient;

	public GitHubAnalysisService(GitHubClient gitHubClient) {
		this.gitHubClient = gitHubClient;
	}

	public AnalysisResponseDto analyze(String username) {
		String normalizedUsername = username == null ? "" : username.strip();
		GitHubProfileDto profile = gitHubClient.getUserProfile(normalizedUsername);
		List<GitHubRepoDto> repos = safeRepos(gitHubClient.getUserRepos(normalizedUsername));

		List<GitHubRepoDto> originals = originalsOnly(repos);
		boolean allReposAreForks = !repos.isEmpty() && originals.isEmpty();

		long totalStars = originals.stream().mapToLong(GitHubRepoDto::stars).sum();
		long totalForks = originals.stream().mapToLong(GitHubRepoDto::forks).sum();

		Map<String, Long> languageCounts = languageCounts(originals);
		List<String> topLanguages = topLanguagesDescending(languageCounts);

		boolean repoCountStrength = originals.size() >= MIN_REPOS_ORIGINAL_MULTIPLICITY_STRENGTH;
		boolean starsStrength = totalStars > 0;
		boolean languagesStrength = distinctLanguageCount(languageCounts) >= MIN_DISTINCT_LANGUAGES_STRENGTH;
		boolean descriptionStrength = hasNonEmptyDescription(originals);
		Instant sixMonthsAgo = sixMonthsAgoUtcInstant();
		boolean recentActivityStrength = hasRecentOriginalActivity(originals, sixMonthsAgo);

		int score = 0;
		if (repoCountStrength) {
			score += SCORE_BONUS_EACH;
		}
		if (starsStrength) {
			score += SCORE_BONUS_EACH;
		}
		if (languagesStrength) {
			score += SCORE_BONUS_EACH;
		}
		if (descriptionStrength) {
			score += SCORE_BONUS_EACH;
		}
		if (recentActivityStrength) {
			score += SCORE_BONUS_EACH;
		}

		List<String> strengths = new ArrayList<>();
		if (repoCountStrength) {
			strengths.add("Has multiple public repositories");
		}
		if (starsStrength) {
			strengths.add("Has repositories with stars");
		}
		if (languagesStrength) {
			strengths.add("Uses several programming languages");
		}
		if (descriptionStrength) {
			strengths.add("Provides descriptions for some repositories");
		}
		if (recentActivityStrength) {
			strengths.add("Has recent GitHub activity");
		}

		List<String> weaknesses = new ArrayList<>();
		if (allReposAreForks) {
			weaknesses.add("Original project signals are limited — all fetched repositories appear to be forks");
		}
		if (!repoCountStrength) {
			weaknesses.add("Has fewer than 5 public repositories");
		}
		if (!starsStrength) {
			weaknesses.add("Repositories have no stars yet");
		}
		if (!languagesStrength) {
			weaknesses.add("Limited language variety");
		}
		if (!descriptionStrength) {
			weaknesses.add("Many repositories are missing descriptions");
		}
		if (!recentActivityStrength) {
			weaknesses.add("No repositories updated in the last 6 months");
		}

		String resolvedLogin = nullToEmpty(profile.login());
		String displayUsername = resolvedLogin.isEmpty() ? normalizedUsername : resolvedLogin;

		return new AnalysisResponseDto(
				displayUsername,
				profile.name(),
				profile.avatarUrl(),
				profile.bio(),
				profile.publicRepos(),
				profile.followers(),
				totalStars,
				totalForks,
				topLanguages,
				List.copyOf(strengths),
				List.copyOf(weaknesses),
				score
		);
	}

	private static List<GitHubRepoDto> safeRepos(List<GitHubRepoDto> repos) {
		return repos != null ? repos : List.of();
	}

	private static List<GitHubRepoDto> originalsOnly(List<GitHubRepoDto> repos) {
		return repos.stream()
				.filter(Objects::nonNull)
				.filter(repo -> !repo.fork())
				.toList();
	}

	private static Map<String, Long> languageCounts(List<GitHubRepoDto> originals) {
		Map<String, Long> counts = new LinkedHashMap<>();
		for (GitHubRepoDto r : originals) {
			String lang = r.language();
			if (lang == null || lang.isBlank()) {
				continue;
			}
			String key = lang.strip();
			if (key.isEmpty()) {
				continue;
			}
			counts.merge(key, 1L, Long::sum);
		}
		return counts;
	}

	private static long distinctLanguageCount(Map<String, Long> languageCounts) {
		return languageCounts.size();
	}

	private static List<String> topLanguagesDescending(Map<String, Long> counts) {
		return counts.entrySet().stream()
				.sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
						.thenComparing(Map.Entry::getKey, Comparator.comparing(s -> s.toLowerCase(Locale.ROOT))))
				.map(Map.Entry::getKey)
				.toList();
	}

	private static boolean hasNonEmptyDescription(List<GitHubRepoDto> originals) {
		for (GitHubRepoDto r : originals) {
			String d = r.description();
			if (d != null && !d.isBlank()) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasRecentOriginalActivity(List<GitHubRepoDto> originals, Instant cutoff) {
		for (GitHubRepoDto r : originals) {
			Instant updated = r.updatedAt();
			if (updated != null && !updated.isBefore(cutoff)) {
				return true;
			}
		}
		return false;
	}

	private static Instant sixMonthsAgoUtcInstant() {
		return ZonedDateTime.now(ZoneOffset.UTC).minusMonths(6).toInstant();
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
