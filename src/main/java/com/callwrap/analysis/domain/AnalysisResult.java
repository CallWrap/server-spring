package com.callwrap.analysis.domain;

import com.callwrap.global.common.Judgment;
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
                case "grade" -> this.grade = withConfirmedValue(this.grade, value);
                case "summary" -> this.summary = withConfirmedValue(this.summary, value);
                case "followup" -> this.needsFollowup = withConfirmedValue(this.needsFollowup, value);
                default -> log.warn("[review] 알 수 없는 confirmedFields 키 무시됨: {}", field);
            }
        });
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
