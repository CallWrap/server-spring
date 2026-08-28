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

    /** 관리자 상담대장 전체 조회. 빈 문자열은 해당 필터를 사용하지 않는다는 뜻이다. */
    @Query(value = """
        SELECT c.*
        FROM calls c
        LEFT JOIN analysis_results ar ON ar.call_id = c.id
        LEFT JOIN complainants cp ON cp.id = c.complainant_id
        LEFT JOIN counselors co ON co.id = c.counselor_id
        WHERE (:status = '' OR c.status = :status)
          AND COALESCE(c.call_started_at, c.created_at) >= :from
          AND COALESCE(c.call_started_at, c.created_at) < :to
          AND (:grade = '' OR ar.grade ->> 'value' = :grade)
          AND (:type = '' OR ar.consultation_type ->> 'value' = :type)
          AND (:counselorId = '' OR CAST(c.counselor_id AS text) = :counselorId)
          AND (
              :query = ''
              OR LOWER(COALESCE(cp.name, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(COALESCE(cp.phone, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(COALESCE(cp.org, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(co.name) LIKE LOWER(CONCAT('%', :query, '%'))
              OR CAST(c.call_no AS text) LIKE CONCAT('%', :query, '%')
              OR LOWER(COALESCE(ar.summary -> 'value' ->> 'title', '')) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY c.updated_at DESC
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM calls c
        LEFT JOIN analysis_results ar ON ar.call_id = c.id
        LEFT JOIN complainants cp ON cp.id = c.complainant_id
        LEFT JOIN counselors co ON co.id = c.counselor_id
        WHERE (:status = '' OR c.status = :status)
          AND COALESCE(c.call_started_at, c.created_at) >= :from
          AND COALESCE(c.call_started_at, c.created_at) < :to
          AND (:grade = '' OR ar.grade ->> 'value' = :grade)
          AND (:type = '' OR ar.consultation_type ->> 'value' = :type)
          AND (:counselorId = '' OR CAST(c.counselor_id AS text) = :counselorId)
          AND (
              :query = ''
              OR LOWER(COALESCE(cp.name, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(COALESCE(cp.phone, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(COALESCE(cp.org, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(co.name) LIKE LOWER(CONCAT('%', :query, '%'))
              OR CAST(c.call_no AS text) LIKE CONCAT('%', :query, '%')
              OR LOWER(COALESCE(ar.summary -> 'value' ->> 'title', '')) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """, nativeQuery = true)
    Page<Call> searchAdminReviews(
        @Param("status") String status,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("grade") String grade,
        @Param("type") String type,
        @Param("counselorId") String counselorId,
        @Param("query") String query,
        Pageable pageable);

    @Query(value = """
        SELECT c.complainant_id AS customerId,
               COUNT(*) AS totalCalls,
               COUNT(*) FILTER (WHERE ar.grade ->> 'value' = '악성콜') AS maliciousCalls,
               MAX(COALESCE(c.call_started_at, c.created_at)) AS lastCalledAt
        FROM calls c
        LEFT JOIN analysis_results ar ON ar.call_id = c.id
        WHERE c.complainant_id IN (:customerIds)
        GROUP BY c.complainant_id
        """, nativeQuery = true)
    List<CustomerSummaryProjection> summarizeCustomers(@Param("customerIds") List<UUID> customerIds);

    @Query(value = """
        SELECT COUNT(*) AS totalCalls,
               COUNT(*) FILTER (WHERE ar.grade ->> 'value' = '악성콜') AS maliciousCalls,
               COUNT(*) FILTER (WHERE ar.grade ->> 'value' = '일반') AS normalCalls,
               COUNT(*) FILTER (WHERE ar.grade ->> 'value' = '칭찬콜') AS praiseCalls,
               MIN(COALESCE(c.call_started_at, c.created_at)) AS firstCalledAt
        FROM calls c
        LEFT JOIN analysis_results ar ON ar.call_id = c.id
        WHERE c.complainant_id = :customerId
        """, nativeQuery = true)
    Optional<CustomerDetailProjection> summarizeCustomer(@Param("customerId") UUID customerId);

    @Query(value = """
        SELECT TO_CHAR(months.month, 'YYYY-MM') AS month,
               COUNT(c.id) AS totalCalls,
               COUNT(c.id) FILTER (WHERE ar.grade ->> 'value' = '악성콜') AS maliciousCalls,
               COUNT(c.id) FILTER (WHERE ar.grade ->> 'value' = '칭찬콜') AS praiseCalls
        FROM GENERATE_SERIES(
            DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '5 months',
            DATE_TRUNC('month', CURRENT_DATE),
            INTERVAL '1 month'
        ) AS months(month)
        LEFT JOIN calls c
          ON c.complainant_id = :customerId
         AND DATE_TRUNC('month', COALESCE(c.call_started_at, c.created_at)) = months.month
        LEFT JOIN analysis_results ar ON ar.call_id = c.id
        GROUP BY months.month
        ORDER BY months.month
        """, nativeQuery = true)
    List<CustomerMonthlyCountProjection> countCustomerCallsByMonth(@Param("customerId") UUID customerId);

    @Query(value = """
        SELECT ar.consultation_subcategory ->> 'value' AS label, COUNT(*) AS cnt
        FROM calls c
        JOIN analysis_results ar ON ar.call_id = c.id
        WHERE c.complainant_id = :customerId
          AND ar.consultation_subcategory ->> 'value' IS NOT NULL
        GROUP BY ar.consultation_subcategory ->> 'value'
        ORDER BY cnt DESC, label
        """, nativeQuery = true)
    List<LabelCountProjection> findCustomerTopSubcategories(
        @Param("customerId") UUID customerId, Pageable pageable);

    @Query(value = """
        SELECT c.id AS reviewId,
               c.call_no AS callNo,
               COALESCE(c.call_started_at, c.created_at) AS calledAt,
               co.name AS counselorName,
               ar.grade ->> 'value' AS grade,
               ar.summary -> 'value' ->> 'title' AS summary,
               c.call_duration_sec AS callDurationSec,
               ar.review_sec AS reviewSec
        FROM calls c
        JOIN counselors co ON co.id = c.counselor_id
        LEFT JOIN analysis_results ar ON ar.call_id = c.id
        WHERE c.complainant_id = :customerId
        ORDER BY COALESCE(c.call_started_at, c.created_at) DESC
        """,
        countQuery = "SELECT COUNT(*) FROM calls c WHERE c.complainant_id = :customerId",
        nativeQuery = true)
    Page<CustomerCallProjection> findCustomerCalls(
        @Param("customerId") UUID customerId, Pageable pageable);

    /**
     * 관리자 대시보드 개요용. 예전엔 목록 API로 최근 N건을 받아와 프론트에서 상태별로 세었는데,
     * 그러면 (1) size 제한 밖의 콜은 통계에서 아예 빠지고 (2) 세는 것뿐인데 JSONB 전체를 다 실어보낸다.
     * idx_calls_status가 이미 있어서 GROUP BY도 그 인덱스를 탄다.
     */
    @Query("SELECT c.status AS status, COUNT(c) AS cnt FROM Call c GROUP BY c.status")
    List<StatusCountProjection> countByStatus();

    /**
     * 상담사 화면 상단바의 "진행 중 / 완료" 카운터용. CallStatus에는 `진행 중`이 없고
     * CONFIRMED + needs_followup=true를 그렇게 쓰기로 했는데(백엔드_요청사항 7번 우회안),
     * countByStatus()는 CONFIRMED를 한 덩어리로만 줘서 관리자 화면에서 둘을 못 나눈다.
     * 확정된 콜만 needs_followup 기준으로 갈라서 센다.
     */
    @Query(value = """
        SELECT COALESCE(ar.needs_followup ->> 'value', 'false') = 'true' AS followup, COUNT(*) AS cnt
        FROM calls c
        LEFT JOIN analysis_results ar ON ar.call_id = c.id
        WHERE c.status = 'CONFIRMED'
        GROUP BY 1
        """, nativeQuery = true)
    List<FollowupSplitProjection> countConfirmedByFollowup();

    interface FollowupSplitProjection {
        boolean getFollowup();
        long getCnt();
    }

    /**
     * 스위퍼용 — 특정 상태로 threshold 이전부터 갇혀 있는 콜.
     * PROCESSING 전이 이후로는 Spring이 이 행을 건드리지 않으므로 updatedAt이 곧 "언제부터 처리 중인가"다.
     * idx_calls_status를 탄다.
     */
    List<Call> findByStatusAndUpdatedAtBefore(CallStatus status, java.time.LocalDateTime threshold);

    /**
     * 관리자 대시보드 "시간대별 처리 추이"용 — 오늘(자정 이후) 접수된 콜을 시간(0~23)별로,
     * 다시 등급별로 센다. 화면이 악성/칭찬/일반 3색 누적 막대라(피그마 17) 총계만 내려주면
     * 프론트가 한 가지 색으로 칠할 수밖에 없어 등급 정보가 통째로 사라진다.
     *
     * 등급이 아직 안 붙은 콜(분석 전·실패)은 '일반'으로 합산한다 — 막대에서 빠지면
     * 이 시간대 총 처리량이 실제보다 적게 보인다.
     */
    @Query(value = """
        SELECT EXTRACT(HOUR FROM c.created_at)::int AS hour,
               COUNT(*) FILTER (WHERE COALESCE(ar.grade ->> 'value', '일반') = '악성콜') AS malicious,
               COUNT(*) FILTER (WHERE COALESCE(ar.grade ->> 'value', '일반') = '칭찬콜') AS praise,
               COUNT(*) FILTER (WHERE COALESCE(ar.grade ->> 'value', '일반') NOT IN ('악성콜', '칭찬콜')) AS normal
        FROM calls c
        LEFT JOIN analysis_results ar ON ar.call_id = c.id
        WHERE c.created_at >= date_trunc('day', now())
        GROUP BY EXTRACT(HOUR FROM c.created_at)
        ORDER BY hour
        """, nativeQuery = true)
    List<HourGradeCountProjection> countTodayByHour();

    interface StatusCountProjection {
        CallStatus getStatus();
        Long getCnt();
    }

    interface HourGradeCountProjection {
        Integer getHour();
        Long getMalicious();
        Long getPraise();
        Long getNormal();
    }

    interface CustomerSummaryProjection {
        UUID getCustomerId();
        long getTotalCalls();
        long getMaliciousCalls();
        LocalDateTime getLastCalledAt();
    }

    interface CustomerDetailProjection {
        long getTotalCalls();
        long getMaliciousCalls();
        long getNormalCalls();
        long getPraiseCalls();
        LocalDateTime getFirstCalledAt();
    }

    interface CustomerMonthlyCountProjection {
        String getMonth();
        long getTotalCalls();
        long getMaliciousCalls();
        long getPraiseCalls();
    }

    interface LabelCountProjection {
        String getLabel();
        long getCnt();
    }

    interface CustomerCallProjection {
        UUID getReviewId();
        int getCallNo();
        LocalDateTime getCalledAt();
        String getCounselorName();
        String getGrade();
        String getSummary();
        Integer getCallDurationSec();
        Integer getReviewSec();
    }
}
