package com.devsignal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsignal.dto.roadmap.AiRoadmapResponseDto;
import com.devsignal.service.OpenAiRoadmapService;

@RestController
@RequestMapping("/api/roadmap")
public class RoadmapController {

	private final OpenAiRoadmapService openAiRoadmapService;

	public RoadmapController(OpenAiRoadmapService openAiRoadmapService) {
		this.openAiRoadmapService = openAiRoadmapService;
	}

	/**
	 * Runs the standard GitHub analysis, then generates an AI-powered improvement roadmap.
	 * Scoring always comes from {@link com.devsignal.service.GitHubAnalysisService}; AI generates the roadmap only.
	 */
	@GetMapping("/{username}")
	public AiRoadmapResponseDto roadmap(@PathVariable String username) {
		return openAiRoadmapService.buildRoadmap(username);
	}
}
