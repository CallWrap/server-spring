package com.callwrap.transcript.repository;

import com.callwrap.transcript.domain.Transcript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {

    List<Transcript> findByCallIdOrderByRetryIterationAsc(UUID callId);

    // 검토 화면은 항상 "가장 최근 시도"만 보여주면 되므로 재시도 이력 중 마지막 한 건만 조회
    Optional<Transcript> findFirstByCallIdOrderByRetryIterationDesc(UUID callId);
}
