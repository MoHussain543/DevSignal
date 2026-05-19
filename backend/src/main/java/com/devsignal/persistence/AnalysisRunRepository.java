package com.devsignal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, UUID> {

	Optional<AnalysisRun> findByRunKey(UUID runKey);

	List<AnalysisRun> findByGithubUsernameNormalizedOrderByCreatedAtDesc(String githubUsernameNormalized);

	Optional<AnalysisRun> findFirstByGithubUsernameNormalizedOrderByCreatedAtDesc(String githubUsernameNormalized);
}
