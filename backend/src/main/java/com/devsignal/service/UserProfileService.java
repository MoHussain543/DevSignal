package com.devsignal.service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsignal.dto.ai.AiReportDto;
import com.devsignal.dto.analysis.AnalysisResponseDto;
import com.devsignal.dto.roadmap.AiRoadmapDto;
import com.devsignal.persistence.UserProfile;
import com.devsignal.persistence.UserProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserProfileService {

	private final UserProfileRepository userProfileRepository;
	private final ObjectMapper objectMapper;

	public UserProfileService(UserProfileRepository userProfileRepository, ObjectMapper objectMapper) {
		this.userProfileRepository = userProfileRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public void saveLatestAnalysis(UUID userId, AnalysisResponseDto analysis) {
		UserProfile profile = getOrCreate(userId);
		applyAnalysis(profile, analysis);
		userProfileRepository.save(profile);
	}

	@Transactional
	public void saveLatestAiReport(UUID userId, AnalysisResponseDto analysis, AiReportDto aiReport) {
		UserProfile profile = getOrCreate(userId);
		applyAnalysis(profile, analysis);
		profile.setLatestAiReportJson(toJson(aiReport));
		userProfileRepository.save(profile);
	}

	@Transactional
	public void saveLatestRoadmap(UUID userId, AnalysisResponseDto analysis, AiRoadmapDto roadmap) {
		UserProfile profile = getOrCreate(userId);
		applyAnalysis(profile, analysis);
		profile.setLatestRoadmapJson(toJson(roadmap));
		userProfileRepository.save(profile);
	}

	private UserProfile getOrCreate(UUID userId) {
		return userProfileRepository.findById(userId).orElseGet(() -> {
			OffsetDateTime now = OffsetDateTime.now();
			return UserProfile.builder()
					.id(userId)
					.createdAt(now)
					.updatedAt(now)
					.build();
		});
	}

	private void applyAnalysis(UserProfile profile, AnalysisResponseDto analysis) {
		profile.setGithubUsername(analysis.username());
		profile.setGithubUsernameNormalized(normalizeUsername(analysis.username()));
		profile.setLatestScore(analysis.score());
		profile.setLatestCandidateLevel(blankToNull(analysis.candidateLevel()));
		profile.setLatestAnalysisJson(toJson(analysis));
		profile.setLastAnalyzedAt(OffsetDateTime.now());
	}

	private JsonNode toJson(Object value) {
		return objectMapper.valueToTree(value);
	}

	private static String normalizeUsername(String username) {
		return username == null ? null : username.strip().toLowerCase(Locale.ROOT);
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.strip();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
