package com.devsignal.service;

import java.util.UUID;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.devsignal.dto.ai.AiReportResponseDto;
import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.dto.job.AiReportJobStatusDto;
import com.devsignal.dto.job.AiRoadmapJobStatusDto;
import com.devsignal.dto.job.AnalysisJobAcceptedDto;
import com.devsignal.dto.job.AnalysisJobStatusDto;
import com.devsignal.dto.roadmap.AiRoadmapResponseDto;
import com.devsignal.persistence.AnalysisRun;

@Service
public class AnalysisJobService {

	private final AnalysisRunService analysisRunService;
	private final GitHubAnalysisService gitHubAnalysisService;
	private final OpenAiReportService openAiReportService;
	private final OpenAiRoadmapService openAiRoadmapService;
	private final UserProfileService userProfileService;
	private final Executor analysisJobExecutor;

	public AnalysisJobService(
			AnalysisRunService analysisRunService,
			GitHubAnalysisService gitHubAnalysisService,
			OpenAiReportService openAiReportService,
			OpenAiRoadmapService openAiRoadmapService,
			UserProfileService userProfileService,
			@Qualifier("analysisJobExecutor") Executor analysisJobExecutor) {
		this.analysisRunService = analysisRunService;
		this.gitHubAnalysisService = gitHubAnalysisService;
		this.openAiReportService = openAiReportService;
		this.openAiRoadmapService = openAiRoadmapService;
		this.userProfileService = userProfileService;
		this.analysisJobExecutor = analysisJobExecutor;
	}

	public AnalysisJobAcceptedDto startAnalysisJob(String username, UUID userId) {
		AnalysisRun run = analysisRunService.createProcessingRun(username);
		analysisJobExecutor.execute(() -> processAnalysisJob(run.getRunKey(), username, userId));
		return AnalysisJobAcceptedDto.from(run.getRunKey(), run.getStatus());
	}

	public AnalysisJobAcceptedDto startReportJob(String username, UUID userId) {
		AnalysisRun run = analysisRunService.createProcessingRun(username);
		analysisJobExecutor.execute(() -> processReportJob(run.getRunKey(), username, userId));
		return AnalysisJobAcceptedDto.from(run.getRunKey(), run.getStatus());
	}

	public AnalysisJobAcceptedDto startRoadmapJob(String username, UUID userId) {
		AnalysisRun run = analysisRunService.createProcessingRun(username);
		analysisJobExecutor.execute(() -> processRoadmapJob(run.getRunKey(), username, userId));
		return AnalysisJobAcceptedDto.from(run.getRunKey(), run.getStatus());
	}

	public AnalysisJobStatusDto getAnalysisJob(UUID runKey) {
		AnalysisRun run = findRequiredRun(runKey);
		return AnalysisJobStatusDto.from(
				run.getRunKey(),
				run.getStatus(),
				run.getErrorMessage(),
				analysisRunService.toAnalysisResponse(run).orElse(null));
	}

	public AiReportJobStatusDto getReportJob(UUID runKey) {
		AnalysisRun run = findRequiredRun(runKey);
		return AiReportJobStatusDto.from(
				run.getRunKey(),
				run.getStatus(),
				run.getErrorMessage(),
				analysisRunService.toAiReportResponse(run).orElse(null));
	}

	public AiRoadmapJobStatusDto getRoadmapJob(UUID runKey) {
		AnalysisRun run = findRequiredRun(runKey);
		return AiRoadmapJobStatusDto.from(
				run.getRunKey(),
				run.getStatus(),
				run.getErrorMessage(),
				analysisRunService.toAiRoadmapResponse(run).orElse(null));
	}

	private void processAnalysisJob(UUID runKey, String username, UUID userId) {
		try {
			AnalysisResponseDto analysis = gitHubAnalysisService.analyze(username);
			analysisRunService.completeAnalysisRun(runKey, analysis);
			saveLatestAnalysis(userId, analysis);
		}
		catch (Exception ex) {
			analysisRunService.markFailed(runKey, ex.getMessage());
		}
	}

	private void processReportJob(UUID runKey, String username, UUID userId) {
		try {
			AnalysisResponseDto analysis = gitHubAnalysisService.analyze(username);
			var aiSummary = openAiReportService.summarizeAnalysis(analysis);
			analysisRunService.completeAiReportRun(runKey, analysis, aiSummary);
			if (userId != null) {
				userProfileService.saveLatestAiReport(userId, analysis, aiSummary);
			}
		}
		catch (Exception ex) {
			analysisRunService.markFailed(runKey, ex.getMessage());
		}
	}

	private void processRoadmapJob(UUID runKey, String username, UUID userId) {
		try {
			AnalysisResponseDto analysis = gitHubAnalysisService.analyze(username);
			var roadmap = openAiRoadmapService.generateRoadmapForAnalysis(analysis);
			analysisRunService.completeRoadmapRun(runKey, analysis, roadmap);
			if (userId != null) {
				userProfileService.saveLatestRoadmap(userId, analysis, roadmap);
			}
		}
		catch (Exception ex) {
			analysisRunService.markFailed(runKey, ex.getMessage());
		}
	}

	private void saveLatestAnalysis(UUID userId, AnalysisResponseDto analysis) {
		if (userId != null) {
			userProfileService.saveLatestAnalysis(userId, analysis);
		}
	}

	private AnalysisRun findRequiredRun(UUID runKey) {
		return analysisRunService.findByRunKey(runKey)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis job not found."));
	}
}
