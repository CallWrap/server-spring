package com.callwrap.analysis.domain;

import com.callwrap.global.common.Judgment;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * AI가 채운 후처리 폼. 콜당 1건(call_id UNIQUE)이며, 필드별로 JSONB를 나눠 담는다
 * — 라벨 체계가 자주 바뀌는 유형/분류/소분류/등급을 각각 독립적으로 갱신·조회할 수 있게 하기 위해서!
 * 대시보드 집계는 V1 마이그레이션의 표현식 인덱스 ((consultation_type ->> 'value'), (grade ->> 'value'))로 커버한다.

 * confirmed_by_counselor_id / confirmed_at은 상담사가 확정하기 전까지 반드시 NULL이다
 * {@link #confirm} 메서드로만 채워지도록 강제해놨다
 */

@Slf4j
@Entity
@Table(name = "analysis_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "call_id", nullable = false, unique = true)
    private UUID callId;

    @Column(name = "confirmed_by_counselor_id")
    private UUID confirmedByCounselorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Judgment<Object> summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "consultation_type", columnDefinition = "jsonb")
    private Judgment<Object> consultationType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "consultation_category", columnDefinition = "jsonb")
    private Judgment<Object> consultationCategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "consultation_subcategory", columnDefinition = "jsonb")
    private Judgment<Object> consultationSubcategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Judgment<Object> grade;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "needs_followup", columnDefinition = "jsonb")
    private Judgment<Object> needsFollowup;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "needs_faq", columnDefinition = "jsonb")
    private Judgment<Object> needsFaq;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<PipelineFailure> failures;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "edited_fields", columnDefinition = "jsonb")
    private List<String> editedFields;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** FAQ 배치 생성에 이미 재료로 쓰였는지. null이면 아직 후보 대상 (FaqCandidateService가 거른다). */
    @Column(name = "faq_processed_at")
    private LocalDateTime faqProcessedAt;

    @Builder
    private AnalysisResult(UUID callId, Judgment<Object> summary, Judgment<Object> consultationType,
                           Judgment<Object> consultationCategory, Judgment<Object> consultationSubcategory,
                           Judgment<Object> grade, Judgment<Object> needsFollowup, Judgment<Object> needsFaq,
                           List<PipelineFailure> failures) {
        this.callId = callId;
        this.summary = summary;
        this.consultationType = consultationType;
        this.consultationCategory = consultationCategory;
        this.consultationSubcategory = consultationSubcategory;
        this.grade = grade;
        this.needsFollowup = needsFollowup;
        this.needsFaq = needsFaq;
        this.failures = failures;
        this.createdAt = LocalDateTime.now();
    }

    public void applyConfirmedFields(Map<String, Object> confirmedFields) {
        if (confirmedFields == null) {
            return;
        }
        confirmedFields.forEach((field, value) -> {
            switch (field) {
                case "type" -> this.consultationType = withConfirmedValue(this.consultationType, value);
                case "category" -> this.consultationCategory = withConfirmedValue(this.consultationCategory, value);
                case "subcategory" -> this.consultationSubcategory = withConfirmedValue(this.consultationSubcategory, value);
                case "grade" -> this.grade = withConfirmedValue(this.grade, value);
                case "summary" -> this.summary = withConfirmedValue(this.summary, requireSummaryShape(value));
                case "followup" -> this.needsFollowup = withConfirmedValue(this.needsFollowup, value);
                case "faq" -> this.needsFaq = withConfirmedValue(this.needsFaq, value);
                default -> log.warn("[review] 알 수 없는 confirmedFields 키 무시됨: {}", field);
            }
        });
    }

    /** FAQ 배치가 이 콜을 재료로 썼음을 표시 — 다음 배치 후보 조회에서 제외된다. */
    public void markFaqProcessed() {
        this.faqProcessedAt = LocalDateTime.now();
    }

    /**
     * 요약 확정값은 반드시 {title, inquiry, resolution} 구조여야 한다.
     *
     * 예전엔 web이 세 값을 "[제목]\n문의: …\n처리: …" 한 덩어리 문자열로 합쳐 보냈고, 이 메서드가
     * 그걸 그대로 받아 JSONB의 구조를 통째로 덮어썼다. 그 순간 대기열 제목(summary->'value'->>'title')이
     * 사라지고 FAQ 후보 조회(summary->'value'->>'inquiry')도 NULL이 됐는데, 화면은 멀쩡해 보여서
     * DB를 열기 전엔 아무도 몰랐다. 조용히 망가지느니 요청을 거절한다.
     */
    private Object requireSummaryShape(Object value) {
        if (value instanceof Map<?, ?> summary
            && summary.get("title") instanceof String
            && summary.get("inquiry") instanceof String
            && summary.get("resolution") instanceof String) {
            return value;
        }
        log.error("[review] summary 확정값이 계약 구조가 아님 (받은 타입: {})",
            value == null ? "null" : value.getClass().getSimpleName());
        throw new CallWrapException(ErrorCode.VALIDATION_ERROR);
    }

    private Judgment<Object> withConfirmedValue(Judgment<Object> original, Object confirmedValue) {
        java.util.List<Integer> evidence = original != null ? original.evidenceIndices() : java.util.List.of();
        return new Judgment<>(confirmedValue, 1.0, evidence);
    }

    @Column(name = "review_sec")
    private Integer reviewSec;

    public void confirm(UUID counselorId, List<String> editedFields, Integer reviewSec) {
        this.confirmedByCounselorId = counselorId;
        this.confirmedAt = LocalDateTime.now();
        this.editedFields = editedFields;
        this.reviewSec = reviewSec;
    }

    public boolean isConfirmed() {
        return confirmedAt != null;
    }
}
