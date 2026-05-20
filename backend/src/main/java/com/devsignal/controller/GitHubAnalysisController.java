package com.devsignal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.service.AnalysisRunService;
import com.devsignal.service.GitHubAnalysisService;
import com.devsignal.service.SupabaseAuthService;
import com.devsignal.service.UserProfileService;

@RestController
@RequestMapping("/api/analyze")
public class GitHubAnalysisController {

	private final GitHubAnalysisService gitHubAnalysisService;
	private final AnalysisRunService analysisRunService;
	private final SupabaseAuthService supabaseAuthService;
	private final UserProfileService userProfileService;

	public GitHubAnalysisController(
			GitHubAnalysisService gitHubAnalysisService,
			AnalysisRunService analysisRunService,
			SupabaseAuthService supabaseAuthService,
			UserProfileService userProfileService) {
		this.gitHubAnalysisService = gitHubAnalysisService;
		this.analysisRunService = analysisRunService;
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
}
