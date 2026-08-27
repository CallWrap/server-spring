package com.callwrap.review.repository;

import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CallRepository extends JpaRepository<Call, UUID> {

    Optional<Call> findBySourceRequestId(String sourceRequestId);

    boolean existsBySourceRequestId(String sourceRequestId);

    /**
     * 상담사 본인 목록만(백엔드_요청사항_상담사화면.md 5번) + 기간 필터(6-2번).
     * idx_calls_counselor_id/idx_calls_status를 그대로 태운다. status가 null이면 그 조건은 무시.
     * 상세 조회(getDetail)는 이 조건을 타지 않는다 — 후속 처리·상담 이력이 상담사 제한 없이 열려야 해서다.
     *
     * from/to는 ReviewQueryService가 항상 값(필터 없으면 아주 넓은 sentinel 범위)을 채워 넘긴다 —
     * PostgreSQL은 "오직 IS NULL 비교에만 쓰이는 파라미터"의 타입을 못 정해서
     * "could not determine data type of parameter"로 죽는다(파라미터가 실제로 null이어도 마찬가지).
     * call_started_at이 비어있는 콜(선택 항목이라 null일 수 있다)도 필터 없을 때는 그대로 나와야
     * 하므로 created_at(항상 값 있음)으로 대체해서 비교한다.
     */
    @Query("SELECT c FROM Call c WHERE c.counselorId = :counselorId "
         + "AND (:status IS NULL OR c.status = :status) "
         + "AND COALESCE(c.callStartedAt, c.createdAt) >= :from "
         + "AND COALESCE(c.callStartedAt, c.createdAt) < :to "
         + "ORDER BY c.updatedAt DESC")
    Page<Call> searchReviews(@Param("counselorId") UUID counselorId, @Param("status") CallStatus status,
                              @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

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

    /** 관리자 대시보드 "시간대별 처리량"용 — 오늘(자정 이후) 접수된 콜을 시간(0~23)별로 센다. */
    @Query(value = """
        SELECT EXTRACT(HOUR FROM created_at)::int AS hour, COUNT(*) AS cnt
        FROM calls
        WHERE created_at >= date_trunc('day', now())
        GROUP BY EXTRACT(HOUR FROM created_at)
        ORDER BY hour
        """, nativeQuery = true)
    List<HourCountProjection> countTodayByHour();

    interface StatusCountProjection {
        CallStatus getStatus();
        Long getCnt();
    }

    interface HourCountProjection {
        Integer getHour();
        Long getCnt();
    }
}
