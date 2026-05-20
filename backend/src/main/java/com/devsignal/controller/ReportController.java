package com.devsignal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsignal.dto.ai.AiReportResponseDto;
import com.devsignal.service.OpenAiReportService;
import com.devsignal.service.SupabaseAuthService;
import com.devsignal.service.UserProfileService;

@RestController
@RequestMapping("/api/report")
public class ReportController {

	private final OpenAiReportService openAiReportService;
	private final SupabaseAuthService supabaseAuthService;
	private final UserProfileService userProfileService;

	public ReportController(
			OpenAiReportService openAiReportService,
			SupabaseAuthService supabaseAuthService,
			UserProfileService userProfileService) {
		this.openAiReportService = openAiReportService;
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
}
