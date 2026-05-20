package com.devsignal.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profiles", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "github_username")
	private String githubUsername;

	@Column(name = "github_username_normalized")
	private String githubUsernameNormalized;

	@Column(name = "latest_score")
	private Integer latestScore;

	@Column(name = "latest_candidate_level")
	private String latestCandidateLevel;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "latest_analysis_json", columnDefinition = "jsonb")
	private JsonNode latestAnalysisJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "latest_ai_report_json", columnDefinition = "jsonb")
	private JsonNode latestAiReportJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "latest_roadmap_json", columnDefinition = "jsonb")
	private JsonNode latestRoadmapJson;

	@Column(name = "last_analyzed_at")
	private OffsetDateTime lastAnalyzedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;
}
