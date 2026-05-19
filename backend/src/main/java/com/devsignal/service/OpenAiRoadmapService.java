package com.devsignal.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devsignal.config.OpenAiProperties;
import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.dto.roadmap.AiRoadmapDto;
import com.devsignal.dto.roadmap.AiRoadmapResponseDto;
import com.devsignal.dto.roadmap.ProjectIdeaDto;
import com.devsignal.dto.roadmap.QuickWinDto;
import com.devsignal.dto.roadmap.SkillDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;

@Service
public class OpenAiRoadmapService {

	private static final Logger log = LoggerFactory.getLogger(OpenAiRoadmapService.class);

	private static final String FALLBACK_MESSAGE =
			"AI roadmap is unavailable right now. Please try again later.";

	private static final ObjectMapper JSON = new ObjectMapper();

	private final GitHubAnalysisService gitHubAnalysisService;
	private final OpenAiProperties properties;

	private volatile OpenAIClient client;

	public OpenAiRoadmapService(GitHubAnalysisService gitHubAnalysisService, OpenAiProperties properties) {
		this.gitHubAnalysisService = gitHubAnalysisService;
		this.properties = properties;
	}

	public AiRoadmapResponseDto buildRoadmap(String username) {
		AnalysisResponseDto analysis = gitHubAnalysisService.analyze(username);
		AiRoadmapDto roadmap = generateRoadmap(analysis);
		return new AiRoadmapResponseDto(analysis, roadmap);
	}

	private AiRoadmapDto generateRoadmap(AnalysisResponseDto analysis) {
		if (!properties.enabled()) {
			return AiRoadmapDto.unavailable("AI features are disabled in configuration.");
		}
		if (!properties.isConfigured()) {
			return AiRoadmapDto.unavailable(
					"Set OPENAI_API_KEY in your environment to enable AI roadmap generation.");
		}

		OpenAIClient openAi = client();
		if (openAi == null) {
			return AiRoadmapDto.unavailable(FALLBACK_MESSAGE);
		}

		try {
			ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
					.model(properties.resolvedModel())
					.addSystemMessage(RoadmapPromptBuilder.systemPrompt())
					.addUserMessage(RoadmapPromptBuilder.userPrompt(analysis))
					.temperature(0.5)
					.build();

			ChatCompletion completion = openAi.chat().completions().create(params);
			String content = extractMessageContent(completion);
			if (content.isBlank()) {
				return AiRoadmapDto.unavailable(FALLBACK_MESSAGE);
			}

			RoadmapPayload payload = parsePayload(content);
			return AiRoadmapDto.available(
					payload.roadmapSummary(),
					toQuickWinDtos(payload.quickWins()),
					toSkillDtos(payload.skillsToLearnNext()),
					toProjectIdeaDtos(payload.nextProjectDirection()),
					payload.highestImpactChange(),
					payload.monthOnePlan(),
					payload.monthTwoPlan(),
					payload.monthThreePlan(),
					payload.expectedOutcome(),
					payload.doList(),
					payload.avoidList());
		}
		catch (Exception ex) {
			log.warn("OpenAI roadmap generation failed for @{}: {}", analysis.username(), ex.getMessage());
			log.debug("OpenAI roadmap failure detail", ex);
			return AiRoadmapDto.unavailable(FALLBACK_MESSAGE);
		}
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

	private RoadmapPayload parsePayload(String raw) throws Exception {
		String json = stripMarkdownFences(raw);
		return JSON.readValue(json, RoadmapPayload.class);
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

	private static List<QuickWinDto> toQuickWinDtos(List<QuickWinPayload> items) {
		if (items == null) return List.of();
		return items.stream()
				.map(q -> new QuickWinDto(q.action(), q.why()))
				.toList();
	}

	private static List<SkillDto> toSkillDtos(List<SkillPayload> items) {
		if (items == null) return List.of();
		return items.stream()
				.map(s -> new SkillDto(s.skill(), s.why(), s.evidenceMissing(), s.howToShow()))
				.toList();
	}

	private static List<ProjectIdeaDto> toProjectIdeaDtos(List<ProjectIdeaPayload> items) {
		if (items == null) return List.of();
		return items.stream()
				.map(p -> new ProjectIdeaDto(
						p.projectName(), p.whyItFits(), p.skillsItProves(),
						p.coreFeatures(), p.whatMakesItImpressive()))
				.toList();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record RoadmapPayload(
			String roadmapSummary,
			List<QuickWinPayload> quickWins,
			List<SkillPayload> skillsToLearnNext,
			List<ProjectIdeaPayload> nextProjectDirection,
			String highestImpactChange,
			String monthOnePlan,
			String monthTwoPlan,
			String monthThreePlan,
			String expectedOutcome,
			List<String> doList,
			List<String> avoidList) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record QuickWinPayload(String action, String why) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record SkillPayload(String skill, String why, String evidenceMissing, String howToShow) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ProjectIdeaPayload(
			String projectName,
			String whyItFits,
			String skillsItProves,
			String coreFeatures,
			String whatMakesItImpressive) {
	}
}
