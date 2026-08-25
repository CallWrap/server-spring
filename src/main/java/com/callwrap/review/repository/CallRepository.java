package com.callwrap.review.repository;

import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CallRepository extends JpaRepository<Call, UUID> {

    Optional<Call> findBySourceRequestId(String sourceRequestId);

    boolean existsBySourceRequestId(String sourceRequestId);

    // status 지정 시 idx_calls_status를 그대로 태운다. status가 null이면 전체 목록(정렬만)
    @Query("SELECT c FROM Call c WHERE (:status IS NULL OR c.status = :status) ORDER BY c.updatedAt DESC")
    Page<Call> searchReviews(@Param("status") CallStatus status, Pageable pageable);

    /**
     * 관리자 대시보드 개요용. 예전엔 목록 API로 최근 N건을 받아와 프론트에서 상태별로 세었는데,
     * 그러면 (1) size 제한 밖의 콜은 통계에서 아예 빠지고 (2) 세는 것뿐인데 JSONB 전체를 다 실어보낸다.
     * idx_calls_status가 이미 있어서 GROUP BY도 그 인덱스를 탄다.
     */
    @Query("SELECT c.status AS status, COUNT(c) AS cnt FROM Call c GROUP BY c.status")
    List<StatusCountProjection> countByStatus();

    /**
     * 관리자 대시보드 "시간대별 처리량" 차트용. 오늘 자정 이후 접수된 콜을 시간(0~23) 단위로 센다.
     * hour는 문자열로 반환 — 프론트가 Record&lt;string, number&gt;로 그대로 받는다.
     */
    @Query(value = """
        SELECT EXTRACT(HOUR FROM c.created_at)::text AS hour, COUNT(*) AS cnt
        FROM calls c
        WHERE c.created_at >= CURRENT_DATE
        GROUP BY EXTRACT(HOUR FROM c.created_at)
        """, nativeQuery = true)
    List<HourCountProjection> hourlyThroughputToday();

    /**
     * 스위퍼용 — 특정 상태로 threshold 이전부터 갇혀 있는 콜.
     * PROCESSING 전이 이후로는 Spring이 이 행을 건드리지 않으므로 updatedAt이 곧 "언제부터 처리 중인가"다.
     * idx_calls_status를 탄다.
     */
    List<Call> findByStatusAndUpdatedAtBefore(CallStatus status, java.time.LocalDateTime threshold);

    interface StatusCountProjection {
        CallStatus getStatus();
        Long getCnt();
    }

    interface HourCountProjection {
        String getHour();
        Long getCnt();
    }
}
