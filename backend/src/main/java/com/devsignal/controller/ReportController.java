package com.devsignal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsignal.dto.ai.AiReportResponseDto;
import com.devsignal.service.OpenAiReportService;

@RestController
@RequestMapping("/api/report")
public class ReportController {

	private final OpenAiReportService openAiReportService;

	public ReportController(OpenAiReportService openAiReportService) {
		this.openAiReportService = openAiReportService;
	}

	/**
	 * Runs the standard GitHub analysis, then optionally adds an OpenAI narrative summary.
	 * Scoring always comes from {@link com.devsignal.service.GitHubAnalysisService}; AI only narrates.
	 */
	@GetMapping("/{username}")
	public AiReportResponseDto report(@PathVariable String username) {
		return openAiReportService.buildReport(username);
	}
}
