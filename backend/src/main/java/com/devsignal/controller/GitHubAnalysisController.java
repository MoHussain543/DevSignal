package com.devsignal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.service.GitHubAnalysisService;

@RestController
@RequestMapping("/api/analyze")
public class GitHubAnalysisController {

	private final GitHubAnalysisService gitHubAnalysisService;

	public GitHubAnalysisController(GitHubAnalysisService gitHubAnalysisService) {
		this.gitHubAnalysisService = gitHubAnalysisService;
	}

	@GetMapping("/{username}")
	public AnalysisResponseDto analyze(@PathVariable String username) {
		return gitHubAnalysisService.analyze(username);
	}
}
