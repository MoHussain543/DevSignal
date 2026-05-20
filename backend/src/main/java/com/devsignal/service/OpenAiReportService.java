package com.devsignal.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devsignal.config.OpenAiProperties;
import com.devsignal.dto.ai.AiPriorityItemDto;
import com.devsignal.dto.ai.AiReportDto;
import com.devsignal.dto.ai.AiReportResponseDto;
import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;

@Service
public class OpenAiReportService {

	private static final Logger log = LoggerFactory.getLogger(OpenAiReportService.class);

	private static final String FALLBACK_MESSAGE =
			"AI summary is unavailable right now. Your scored report below is complete and unchanged.";

	private static final ObjectMapper JSON = new ObjectMapper();

	private final GitHubAnalysisService gitHubAnalysisService;
	private final AnalysisRunService analysisRunService;
	private final OpenAiProperties properties;

	private volatile OpenAIClient client;

	public OpenAiReportService(
			GitHubAnalysisService gitHubAnalysisService,
			AnalysisRunService analysisRunService,
			OpenAiProperties properties) {
		this.gitHubAnalysisService = gitHubAnalysisService;
		this.analysisRunService = analysisRunService;
		this.properties = properties;
	}

	public AiReportResponseDto buildReport(String username) {
		AnalysisResponseDto analysis = gitHubAnalysisService.analyze(username);
		AiReportDto aiSummary = summarizeAnalysis(analysis);
		analysisRunService.saveAiReport(analysis, aiSummary);
		return new AiReportResponseDto(analysis, aiSummary);
	}

	public AiReportDto summarizeAnalysis(AnalysisResponseDto analysis) {
		if (!properties.enabled()) {
			return AiReportDto.unavailable("AI summaries are disabled in configuration.");
		}
		if (!properties.isConfigured()) {
			return AiReportDto.unavailable(
					"Set OPENAI_API_KEY in your environment to enable AI summaries.");
		}

		OpenAIClient openAi = client();
		if (openAi == null) {
			return AiReportDto.unavailable(FALLBACK_MESSAGE);
		}

		try {
			ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
					.model(properties.resolvedModel())
					.addSystemMessage(ReportPromptBuilder.systemPrompt())
					.addUserMessage(ReportPromptBuilder.userPrompt(analysis))
					.temperature(0.4)
					.build();

			ChatCompletion completion = openAi.chat().completions().create(params);
			String content = extractMessageContent(completion);
			if (content.isBlank()) {
				return AiReportDto.unavailable(FALLBACK_MESSAGE);
			}

			AiSummaryPayload payload = parsePayload(content);
			return AiReportDto.available(
					payload.overallSummary(),
					payload.overallRead(),
					payload.hiringSignal(),
					payload.mainGap(),
					payload.bestSignal(),
					payload.howProfileReadsPattern(),
					payload.howProfileReadsDrivers(),
					payload.howProfileReadsHelps(),
					payload.howProfileReadsHoldsBack(),
					payload.hiringImpression(),
					payload.whatStandsOut(),
					payload.whatWeakens(),
					payload.positiveSignals(),
					payload.warningSignals(),
					payload.missingSignals(),
					mapPriorities(payload.topPriorities()),
					payload.biggestUnlock());
		}
		catch (Exception ex) {
			log.warn("OpenAI summary failed for @{}: {}", analysis.username(), ex.getMessage());
			log.debug("OpenAI summary failure detail", ex);
			return AiReportDto.unavailable(FALLBACK_MESSAGE);
		}
	}

	private static List<AiPriorityItemDto> mapPriorities(List<PriorityPayload> items) {
		if (items == null || items.isEmpty()) {
			return List.of();
		}
		return items.stream()
				.limit(3)
				.map(p -> new AiPriorityItemDto(p.action(), p.whyItMatters(), p.visibleImprovement()))
				.toList();
	}

	private OpenAIClient client() {
		if (!properties.isConfigured()) {
			return null;
		}
		OpenAIClient existing = client;
		if (existing != null) {
			return existing;
		}
		synchronized (this) {
			if (client == null) {
				client = OpenAIOkHttpClient.builder()
						.apiKey(properties.apiKey().strip())
						.build();
			}
			return client;
		}
	}

	private static String extractMessageContent(ChatCompletion completion) {
		if (completion.choices() == null || completion.choices().isEmpty()) {
			return "";
		}
		ChatCompletionMessage message = completion.choices().getFirst().message();
		if (message == null) {
			return "";
		}
		return message.content().orElse("").strip();
	}

	private AiSummaryPayload parsePayload(String raw) throws Exception {
		String json = stripMarkdownFences(raw);
		return JSON.readValue(json, AiSummaryPayload.class);
	}

	private static String stripMarkdownFences(String raw) {
		String trimmed = raw.strip();
		if (trimmed.startsWith("```")) {
			int firstNewline = trimmed.indexOf('\n');
			int lastFence = trimmed.lastIndexOf("```");
			if (firstNewline > 0 && lastFence > firstNewline) {
				return trimmed.substring(firstNewline + 1, lastFence).strip();
			}
		}
		return trimmed;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record PriorityPayload(
			String action,
			String whyItMatters,
			String visibleImprovement) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record AiSummaryPayload(
			String overallSummary,
			String overallRead,
			String hiringSignal,
			String mainGap,
			String bestSignal,
			String howProfileReadsPattern,
			String howProfileReadsDrivers,
			String howProfileReadsHelps,
			String howProfileReadsHoldsBack,
			String hiringImpression,
			String whatStandsOut,
			String whatWeakens,
			List<String> positiveSignals,
			List<String> warningSignals,
			List<String> missingSignals,
			List<PriorityPayload> topPriorities,
			String biggestUnlock) {
	}

}
