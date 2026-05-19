package com.devsignal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.service.AnalysisRunService;
import com.devsignal.service.GitHubAnalysisService;

@RestController
@RequestMapping("/api/analyze")
public class GitHubAnalysisController {

	private final GitHubAnalysisService gitHubAnalysisService;
	private final AnalysisRunService analysisRunService;

	public GitHubAnalysisController(
			GitHubAnalysisService gitHubAnalysisService,
			AnalysisRunService analysisRunService) {
		this.gitHubAnalysisService = gitHubAnalysisService;
		this.analysisRunService = analysisRunService;
	}

	@GetMapping("/{username}")
	public AnalysisResponseDto analyze(@PathVariable String username) {
		AnalysisResponseDto analysis = gitHubAnalysisService.analyze(username);
		analysisRunService.createAnalysisRun(analysis);
		return analysis;
	}
}
