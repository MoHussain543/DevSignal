package com.devsignal.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsignal.dto.ai.AiReportResponseDto;
import com.devsignal.dto.job.AiReportJobStatusDto;
import com.devsignal.dto.job.AnalysisJobAcceptedDto;
import com.devsignal.service.AnalysisJobService;
import com.devsignal.service.OpenAiReportService;
import com.devsignal.service.SupabaseAuthService;
import com.devsignal.service.UserProfileService;

@RestController
@RequestMapping("/api/report")
public class ReportController {

	private final OpenAiReportService openAiReportService;
	private final AnalysisJobService analysisJobService;
	private final SupabaseAuthService supabaseAuthService;
	private final UserProfileService userProfileService;

	public ReportController(
			OpenAiReportService openAiReportService,
			AnalysisJobService analysisJobService,
			SupabaseAuthService supabaseAuthService,
			UserProfileService userProfileService) {
		this.openAiReportService = openAiReportService;
		this.analysisJobService = analysisJobService;
		this.supabaseAuthService = supabaseAuthService;
		this.userProfileService = userProfileService;
	}

	/**
	 * Runs the standard GitHub analysis, then optionally adds an OpenAI narrative summary.
	 * Scoring always comes from {@link com.devsignal.service.GitHubAnalysisService}; AI only narrates.
	 */
	@GetMapping("/{username}")
	public AiReportResponseDto report(
			@PathVariable String username,
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		AiReportResponseDto response = openAiReportService.buildReport(username);
		supabaseAuthService.resolveAuthenticatedUser(authorizationHeader)
				.ifPresent(user -> userProfileService.saveLatestAiReport(user.id(), response.analysis(), response.aiSummary()));
		return response;
	}

	@PostMapping("/{username}/jobs")
	public AnalysisJobAcceptedDto createReportJob(
			@PathVariable String username,
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		UUID userId = supabaseAuthService.resolveAuthenticatedUser(authorizationHeader)
				.map(SupabaseAuthService.AuthenticatedSupabaseUser::id)
				.orElse(null);
		return analysisJobService.startReportJob(username, userId);
	}

	@GetMapping("/jobs/{runKey}")
	public AiReportJobStatusDto getReportJob(@PathVariable UUID runKey) {
		return analysisJobService.getReportJob(runKey);
	}
}
