package com.devsignal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsignal.dto.roadmap.AiRoadmapResponseDto;
import com.devsignal.service.OpenAiRoadmapService;
import com.devsignal.service.SupabaseAuthService;
import com.devsignal.service.UserProfileService;

@RestController
@RequestMapping("/api/roadmap")
public class RoadmapController {

	private final OpenAiRoadmapService openAiRoadmapService;
	private final SupabaseAuthService supabaseAuthService;
	private final UserProfileService userProfileService;

	public RoadmapController(
			OpenAiRoadmapService openAiRoadmapService,
			SupabaseAuthService supabaseAuthService,
			UserProfileService userProfileService) {
		this.openAiRoadmapService = openAiRoadmapService;
		this.supabaseAuthService = supabaseAuthService;
		this.userProfileService = userProfileService;
	}

	/**
	 * Runs the standard GitHub analysis, then generates an AI-powered improvement roadmap.
	 * Scoring always comes from {@link com.devsignal.service.GitHubAnalysisService}; AI generates the roadmap only.
	 */
	@GetMapping("/{username}")
	public AiRoadmapResponseDto roadmap(
			@PathVariable String username,
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		AiRoadmapResponseDto response = openAiRoadmapService.buildRoadmap(username);
		supabaseAuthService.resolveAuthenticatedUser(authorizationHeader)
				.ifPresent(user -> userProfileService.saveLatestRoadmap(user.id(), response.analysis(), response.roadmap()));
		return response;
	}
}
