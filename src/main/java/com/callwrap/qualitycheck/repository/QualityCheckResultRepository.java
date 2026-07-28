package com.callwrap.qualitycheck.repository;

import com.callwrap.qualitycheck.domain.QualityCheckResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QualityCheckResultRepository extends JpaRepository<QualityCheckResult, UUID> {

    List<QualityCheckResult> findByCallIdAndRetryIteration(UUID callId, int retryIteration);

    List<QualityCheckResult> findByCallIdOrderByRetryIterationAscCheckedAtAsc(UUID callId);
}
