package com.devsignal.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsignal.dto.ai.AiReportDto;
import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.dto.roadmap.AiRoadmapDto;
import com.devsignal.persistence.AnalysisRun;
import com.devsignal.persistence.AnalysisRunRepository;
import com.devsignal.persistence.AnalysisRunStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AnalysisRunService {

	private final AnalysisRunRepository analysisRunRepository;
	private final ObjectMapper objectMapper;

	public AnalysisRunService(AnalysisRunRepository analysisRunRepository, ObjectMapper objectMapper) {
		this.analysisRunRepository = analysisRunRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public AnalysisRun createAnalysisRun(AnalysisResponseDto analysis) {
		OffsetDateTime now = OffsetDateTime.now();
		AnalysisRun run = AnalysisRun.builder()
				.id(UUID.randomUUID())
				.runKey(UUID.randomUUID())
				.githubUsername(analysis.username())
				.githubUsernameNormalized(normalizeUsername(analysis.username()))
				.githubDisplayName(blankToNull(analysis.name()))
				.overallScore(analysis.score())
				.candidateLabel(blankToNull(analysis.candidateLevel()))
				.status(AnalysisRunStatus.COMPLETED)
				.errorMessage(null)
				.publicReposCount(analysis.publicRepos())
				.primaryLanguage(resolvePrimaryLanguage(analysis.portfolioTopLanguages()))
				.analysisJson(toJson(analysis))
				.aiReportJson(null)
				.roadmapJson(null)
				.createdAt(now)
				.updatedAt(now)
				.build();

		return analysisRunRepository.save(run);
	}

	@Transactional
	public AnalysisRun saveAiReport(AnalysisResponseDto analysis, AiReportDto aiReport) {
		AnalysisRun run = findLatestRunForUsername(analysis.username())
				.orElseGet(() -> createAnalysisRun(analysis));

		refreshBaseAnalysis(run, analysis);
		run.setAiReportJson(toJson(aiReport));
		applyAiStatus(run, aiReport.available(), aiReport.unavailableReason());
		return analysisRunRepository.save(run);
	}

	@Transactional
	public AnalysisRun saveRoadmap(AnalysisResponseDto analysis, AiRoadmapDto roadmap) {
		AnalysisRun run = findLatestRunForUsername(analysis.username())
				.orElseGet(() -> createAnalysisRun(analysis));

		refreshBaseAnalysis(run, analysis);
		run.setRoadmapJson(toJson(roadmap));
		applyAiStatus(run, roadmap.available(), roadmap.unavailableReason());
		return analysisRunRepository.save(run);
	}

	private java.util.Optional<AnalysisRun> findLatestRunForUsername(String username) {
		return analysisRunRepository.findFirstByGithubUsernameNormalizedOrderByCreatedAtDesc(
				normalizeUsername(username));
	}

	private void refreshBaseAnalysis(AnalysisRun run, AnalysisResponseDto analysis) {
		run.setGithubUsername(analysis.username());
		run.setGithubUsernameNormalized(normalizeUsername(analysis.username()));
		run.setGithubDisplayName(blankToNull(analysis.name()));
		run.setOverallScore(analysis.score());
		run.setCandidateLabel(blankToNull(analysis.candidateLevel()));
		run.setPublicReposCount(analysis.publicRepos());
		run.setPrimaryLanguage(resolvePrimaryLanguage(analysis.portfolioTopLanguages()));
		run.setAnalysisJson(toJson(analysis));
	}

	private void applyAiStatus(AnalysisRun run, boolean available, String unavailableReason) {
		if (available) {
			run.setStatus(AnalysisRunStatus.COMPLETED);
			run.setErrorMessage(null);
			return;
		}
		run.setStatus(AnalysisRunStatus.PARTIAL);
		run.setErrorMessage(blankToNull(unavailableReason));
	}

	private JsonNode toJson(Object value) {
		return objectMapper.valueToTree(value);
	}

	private static String normalizeUsername(String username) {
		return username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
	}

	private static String resolvePrimaryLanguage(List<String> languages) {
		if (languages == null || languages.isEmpty()) {
			return null;
		}
		return blankToNull(languages.getFirst());
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.strip();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
