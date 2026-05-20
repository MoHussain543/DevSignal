package com.devsignal.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsignal.dto.ai.AiReportDto;
import com.devsignal.dto.ai.AiReportResponseDto;
import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.dto.roadmap.AiRoadmapDto;
import com.devsignal.dto.roadmap.AiRoadmapResponseDto;
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
	public AnalysisRun createProcessingRun(String username) {
		String normalizedUsername = normalizeUsername(username);
		if (normalizedUsername.isEmpty()) {
			throw new IllegalArgumentException("GitHub username must not be blank");
		}

		OffsetDateTime now = OffsetDateTime.now();
		AnalysisRun run = AnalysisRun.builder()
				.id(UUID.randomUUID())
				.runKey(UUID.randomUUID())
				.githubUsername(username == null ? "" : username.strip())
				.githubUsernameNormalized(normalizedUsername)
				.status(AnalysisRunStatus.PROCESSING)
				.errorMessage(null)
				.analysisJson(objectMapper.createObjectNode())
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

	@Transactional
	public AnalysisRun completeAnalysisRun(UUID runKey, AnalysisResponseDto analysis) {
		AnalysisRun run = getRequiredRun(runKey);
		refreshBaseAnalysis(run, analysis);
		run.setStatus(AnalysisRunStatus.COMPLETED);
		run.setErrorMessage(null);
		touch(run);
		return analysisRunRepository.save(run);
	}

	@Transactional
	public AnalysisRun completeAiReportRun(UUID runKey, AnalysisResponseDto analysis, AiReportDto aiReport) {
		AnalysisRun run = getRequiredRun(runKey);
		refreshBaseAnalysis(run, analysis);
		run.setAiReportJson(toJson(aiReport));
		applyAiStatus(run, aiReport.available(), aiReport.unavailableReason());
		touch(run);
		return analysisRunRepository.save(run);
	}

	@Transactional
	public AnalysisRun completeRoadmapRun(UUID runKey, AnalysisResponseDto analysis, AiRoadmapDto roadmap) {
		AnalysisRun run = getRequiredRun(runKey);
		refreshBaseAnalysis(run, analysis);
		run.setRoadmapJson(toJson(roadmap));
		applyAiStatus(run, roadmap.available(), roadmap.unavailableReason());
		touch(run);
		return analysisRunRepository.save(run);
	}

	@Transactional
	public AnalysisRun markFailed(UUID runKey, String message) {
		AnalysisRun run = getRequiredRun(runKey);
		run.setStatus(AnalysisRunStatus.FAILED);
		run.setErrorMessage(blankToNull(message));
		touch(run);
		return analysisRunRepository.save(run);
	}

	public Optional<AnalysisRun> findByRunKey(UUID runKey) {
		return analysisRunRepository.findByRunKey(runKey);
	}

	public Optional<AnalysisResponseDto> toAnalysisResponse(AnalysisRun run) {
		if (run == null || run.getOverallScore() == null || run.getAnalysisJson() == null || run.getAnalysisJson().isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(convert(run.getAnalysisJson(), AnalysisResponseDto.class));
	}

	public Optional<AiReportResponseDto> toAiReportResponse(AnalysisRun run) {
		Optional<AnalysisResponseDto> analysis = toAnalysisResponse(run);
		if (analysis.isEmpty()) {
			return Optional.empty();
		}
		AiReportDto aiReport = run.getAiReportJson() == null ? null : convert(run.getAiReportJson(), AiReportDto.class);
		return Optional.of(new AiReportResponseDto(analysis.get(), aiReport));
	}

	public Optional<AiRoadmapResponseDto> toAiRoadmapResponse(AnalysisRun run) {
		Optional<AnalysisResponseDto> analysis = toAnalysisResponse(run);
		if (analysis.isEmpty()) {
			return Optional.empty();
		}
		AiRoadmapDto roadmap = run.getRoadmapJson() == null ? null : convert(run.getRoadmapJson(), AiRoadmapDto.class);
		return Optional.of(new AiRoadmapResponseDto(analysis.get(), roadmap));
	}

	private Optional<AnalysisRun> findLatestRunForUsername(String username) {
		return analysisRunRepository.findFirstByGithubUsernameNormalizedOrderByCreatedAtDesc(
				normalizeUsername(username));
	}

	private AnalysisRun getRequiredRun(UUID runKey) {
		return analysisRunRepository.findByRunKey(runKey)
				.orElseThrow(() -> new IllegalArgumentException("Analysis run not found for key: " + runKey));
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
		touch(run);
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

	private <T> T convert(JsonNode value, Class<T> type) {
		try {
			return objectMapper.treeToValue(value, type);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not deserialize persisted analysis run payload.", ex);
		}
	}

	private static void touch(AnalysisRun run) {
		run.setUpdatedAt(OffsetDateTime.now());
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
