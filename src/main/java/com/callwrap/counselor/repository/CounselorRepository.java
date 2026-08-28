package com.callwrap.counselor.repository;

import com.callwrap.counselor.domain.Counselor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CounselorRepository extends JpaRepository<Counselor, UUID> {

    Optional<Counselor> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    @Query(value = """
        WITH call_metrics AS (
            SELECT c.id AS call_id,
                   c.counselor_id,
                   DATE(COALESCE(c.call_started_at, c.created_at)) AS called_date,
                   c.call_duration_sec,
                   ar.review_sec,
                   ar.grade ->> 'value' AS grade,
                   c.counselor_negative_score AS negative_score
            FROM calls c
            LEFT JOIN analysis_results ar ON ar.call_id = c.id
        )
        SELECT co.id AS counselorId,
               co.name AS name,
               co.team AS team,
               COUNT(cm.call_id) FILTER (WHERE cm.called_date = :date) AS totalCalls,
               COUNT(cm.call_id) FILTER (WHERE cm.called_date = :date AND cm.grade = '악성콜') AS maliciousCalls,
               AVG(cm.call_duration_sec) FILTER (WHERE cm.called_date = :date) AS avgCallSec,
               AVG(cm.review_sec) FILTER (WHERE cm.called_date = :date) AS avgReviewSec,
               AVG(cm.negative_score) FILTER (WHERE cm.called_date BETWEEN :healthFrom AND :healthTo) AS healthNegativeScore,
               COUNT(cm.call_id) FILTER (
                   WHERE cm.called_date BETWEEN :healthFrom AND :healthTo AND cm.grade = '악성콜'
               ) AS healthMaliciousCalls
        FROM counselors co
        LEFT JOIN call_metrics cm ON cm.counselor_id = co.id
        WHERE co.role = 'COUNSELOR'
        GROUP BY co.id, co.name, co.team
        ORDER BY totalCalls DESC, co.name
        """, nativeQuery = true)
    List<CounselorSummaryProjection> findAdminSummaries(
        @Param("date") LocalDate date,
        @Param("healthFrom") LocalDate healthFrom,
        @Param("healthTo") LocalDate healthTo);

    @Query(value = """
        WITH call_metrics AS (
            SELECT c.id AS call_id,
                   c.counselor_id,
                   DATE(COALESCE(c.call_started_at, c.created_at)) AS called_date,
                   c.call_duration_sec,
                   ar.grade ->> 'value' AS grade,
                   c.counselor_negative_score AS negative_score
            FROM calls c
            LEFT JOIN analysis_results ar ON ar.call_id = c.id
        )
        SELECT co.id AS counselorId,
               co.name AS name,
               co.team AS team,
               COUNT(cm.call_id) FILTER (WHERE cm.called_date = :date) AS totalCalls,
               COALESCE(SUM(cm.call_duration_sec) FILTER (WHERE cm.called_date = :date), 0) AS totalCallSec,
               COUNT(cm.call_id) FILTER (WHERE cm.called_date = :date AND cm.grade = '악성콜') AS maliciousCalls,
               COUNT(cm.call_id) FILTER (WHERE cm.called_date = :date AND cm.grade = '일반') AS normalCalls,
               COUNT(cm.call_id) FILTER (WHERE cm.called_date = :date AND cm.grade = '칭찬콜') AS praiseCalls,
               AVG(cm.negative_score) FILTER (WHERE cm.called_date BETWEEN :healthFrom AND :healthTo) AS healthNegativeScore,
               COUNT(cm.call_id) FILTER (
                   WHERE cm.called_date BETWEEN :healthFrom AND :healthTo AND cm.grade = '악성콜'
               ) AS healthMaliciousCalls
        FROM counselors co
        LEFT JOIN call_metrics cm ON cm.counselor_id = co.id
        WHERE co.id = :counselorId AND co.role = 'COUNSELOR'
        GROUP BY co.id, co.name, co.team
        """, nativeQuery = true)
    Optional<CounselorDetailProjection> findAdminDetail(
        @Param("counselorId") UUID counselorId,
        @Param("date") LocalDate date,
        @Param("healthFrom") LocalDate healthFrom,
        @Param("healthTo") LocalDate healthTo);

    @Query(value = """
        WITH call_sentiments AS (
            SELECT c.id AS call_id,
                   DATE(COALESCE(c.call_started_at, c.created_at)) AS called_date,
                   c.counselor_negative_score AS negative_score
            FROM calls c
            WHERE c.counselor_id = :counselorId
        )
        SELECT days.day::date AS date, COALESCE(AVG(cs.negative_score), 0) AS negativeScore
        FROM GENERATE_SERIES(
            CAST(:fromDate AS date), CAST(:toDate AS date), INTERVAL '1 day'
        ) days(day)
        LEFT JOIN call_sentiments cs ON cs.called_date = days.day::date
        GROUP BY days.day
        ORDER BY days.day
        """, nativeQuery = true)
    List<SentimentTrendProjection> findSentimentTrend(
        @Param("counselorId") UUID counselorId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);

    @Query(value = """
        SELECT c.id AS reviewId,
               c.call_no AS callNo,
               COALESCE(c.call_started_at, c.created_at) AS calledAt,
               cp.name AS customerName,
               ar.grade ->> 'value' AS grade,
               ar.summary -> 'value' ->> 'title' AS summary,
               c.call_duration_sec AS callDurationSec,
               ar.review_sec AS reviewSec,
               c.counselor_negative_score AS negativeScore
        FROM calls c
        LEFT JOIN complainants cp ON cp.id = c.complainant_id
        LEFT JOIN analysis_results ar ON ar.call_id = c.id
        WHERE c.counselor_id = :counselorId
          AND DATE(COALESCE(c.call_started_at, c.created_at)) = :date
        ORDER BY COALESCE(c.call_started_at, c.created_at) DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM calls c
        WHERE c.counselor_id = :counselorId
          AND DATE(COALESCE(c.call_started_at, c.created_at)) = :date
        """, nativeQuery = true)
    Page<CounselorCallProjection> findCounselorCalls(
        @Param("counselorId") UUID counselorId,
        @Param("date") LocalDate date,
        Pageable pageable);

    interface CounselorSummaryProjection {
        UUID getCounselorId();
        String getName();
        String getTeam();
        long getTotalCalls();
        long getMaliciousCalls();
        Double getAvgCallSec();
        Double getAvgReviewSec();
        Double getHealthNegativeScore();
        long getHealthMaliciousCalls();
    }

    interface CounselorDetailProjection {
        UUID getCounselorId();
        String getName();
        String getTeam();
        long getTotalCalls();
        long getTotalCallSec();
        long getMaliciousCalls();
        long getNormalCalls();
        long getPraiseCalls();
        Double getHealthNegativeScore();
        long getHealthMaliciousCalls();
    }

    interface SentimentTrendProjection {
        LocalDate getDate();
        Double getNegativeScore();
    }

    interface CounselorCallProjection {
        UUID getReviewId();
        int getCallNo();
        LocalDateTime getCalledAt();
        String getCustomerName();
        String getGrade();
        String getSummary();
        Integer getCallDurationSec();
        Integer getReviewSec();
        Double getNegativeScore();
    }
}
