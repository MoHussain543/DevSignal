package com.devsignal.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.dto.job.AnalysisJobAcceptedDto;
import com.devsignal.dto.job.AnalysisJobStatusDto;
import com.devsignal.service.AnalysisRunService;
import com.devsignal.service.AnalysisJobService;
import com.devsignal.service.GitHubAnalysisService;
import com.devsignal.service.SupabaseAuthService;
import com.devsignal.service.UserProfileService;

@RestController
@RequestMapping("/api/analyze")
public class GitHubAnalysisController {

	private final GitHubAnalysisService gitHubAnalysisService;
	private final AnalysisRunService analysisRunService;
	private final AnalysisJobService analysisJobService;
	private final SupabaseAuthService supabaseAuthService;
	private final UserProfileService userProfileService;

	public GitHubAnalysisController(
			GitHubAnalysisService gitHubAnalysisService,
			AnalysisRunService analysisRunService,
			AnalysisJobService analysisJobService,
			SupabaseAuthService supabaseAuthService,
			UserProfileService userProfileService) {
		this.gitHubAnalysisService = gitHubAnalysisService;
		this.analysisRunService = analysisRunService;
		this.analysisJobService = analysisJobService;
		this.supabaseAuthService = supabaseAuthService;
		this.userProfileService = userProfileService;
	}

	@GetMapping("/{username}")
	public AnalysisResponseDto analyze(
			@PathVariable String username,
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		AnalysisResponseDto analysis = gitHubAnalysisService.analyze(username);
		analysisRunService.createAnalysisRun(analysis);
		supabaseAuthService.resolveAuthenticatedUser(authorizationHeader)
				.ifPresent(user -> userProfileService.saveLatestAnalysis(user.id(), analysis));
		return analysis;
	}

	@PostMapping("/{username}/jobs")
	public AnalysisJobAcceptedDto createAnalysisJob(
			@PathVariable String username,
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		UUID userId = supabaseAuthService.resolveAuthenticatedUser(authorizationHeader)
				.map(SupabaseAuthService.AuthenticatedSupabaseUser::id)
				.orElse(null);
		return analysisJobService.startAnalysisJob(username, userId);
	}

	@GetMapping("/jobs/{runKey}")
	public AnalysisJobStatusDto getAnalysisJob(@PathVariable UUID runKey) {
		return analysisJobService.getAnalysisJob(runKey);
	}
}
