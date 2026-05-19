package com.devsignal.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "analysis_runs", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisRun {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "run_key", nullable = false, unique = true)
	private UUID runKey;

	@Column(name = "github_username", nullable = false)
	private String githubUsername;

	@Column(name = "github_username_normalized", nullable = false)
	private String githubUsernameNormalized;

	@Column(name = "github_display_name")
	private String githubDisplayName;

	@Column(name = "overall_score")
	private Integer overallScore;

	@Column(name = "candidate_label")
	private String candidateLabel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AnalysisRunStatus status;

	@Column(name = "error_message")
	private String errorMessage;

	@Column(name = "public_repos_count")
	private Integer publicReposCount;

	@Column(name = "primary_language")
	private String primaryLanguage;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "analysis_json", nullable = false, columnDefinition = "jsonb")
	private JsonNode analysisJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ai_report_json", columnDefinition = "jsonb")
	private JsonNode aiReportJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "roadmap_json", columnDefinition = "jsonb")
	private JsonNode roadmapJson;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;
}
