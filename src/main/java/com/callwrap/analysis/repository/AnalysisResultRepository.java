package com.callwrap.analysis.repository;

import com.callwrap.analysis.domain.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {

    Optional<AnalysisResult> findByCallId(UUID callId);

    List<AnalysisResult> findByCallIdIn(List<UUID> callIds);
}
