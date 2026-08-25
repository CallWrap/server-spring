package com.callwrap.faq.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 * EXAONE이 needs_faq=true로 쌓인 콜들을 소분류별로 묶어 합성한 FAQ 후보 1건.
 * 생성 직후엔 항상 DRAFT다 — LLM이 만든 문항을 검수 없이 바로 공개하면 안 되므로
 * (web의 "AI 확신도 낮으면 사람이 확인" 패턴과 동일한 이유), 상담사/관리자가
 * PUBLISHED로 승인해야 지식베이스에 실제로 노출된다.
 */
@Entity
@Table(name = "faq_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String subcategory;

    @Column(columnDefinition = "text", nullable = false)
    private String question;

    @Column(columnDefinition = "text", nullable = false)
    private String answer;

    @Column(name = "source_call_count", nullable = false)
    private Integer sourceCallCount;

    /** 이 FAQ 합성에 실제로 쓰인 콜 id들 — 근거 추적용. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sample_call_ids", columnDefinition = "jsonb")
    private List<UUID> sampleCallIds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FaqStatus status;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "reviewed_by_counselor_id")
    private UUID reviewedByCounselorId;

    private FaqEntry(String subcategory, String question, String answer,
                      Integer sourceCallCount, List<UUID> sampleCallIds) {
        this.subcategory = subcategory;
        this.question = question;
        this.answer = answer;
        this.sourceCallCount = sourceCallCount;
        this.sampleCallIds = sampleCallIds;
        this.status = FaqStatus.DRAFT;
        this.generatedAt = LocalDateTime.now();
    }

    public static FaqEntry draft(String subcategory, String question, String answer,
                                  Integer sourceCallCount, List<UUID> sampleCallIds) {
        return new FaqEntry(subcategory, question, answer, sourceCallCount, sampleCallIds);
    }

    /** 검수 중 문항을 상담사가 직접 다듬을 수 있다. DRAFT 상태에서만 의미가 있다(강제하진 않음). */
    public void edit(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public void publish(UUID counselorId) {
        this.status = FaqStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.reviewedByCounselorId = counselorId;
    }

    public void discard(UUID counselorId) {
        this.status = FaqStatus.DISCARDED;
        this.reviewedByCounselorId = counselorId;
    }

    /**
     * 새 배치에서 같은 FAQ로 판단된(주제·질문이 겹치는) 콜들을 기존 항목에 합친다 — 매번 새 DRAFT를
     * 만드는 대신 근거만 누적한다. 질문/답변 문항 자체는 절대 안 건드린다: PUBLISHED된 문항을 배치가
     * 돌 때마다 조용히 바꾸면 안 되고, DRAFT도 상담사가 이미 검수를 시작했을 수 있어서다.
     */
    public void absorb(int additionalCallCount, List<UUID> additionalCallIds) {
        this.sourceCallCount += additionalCallCount;
        List<UUID> merged = new ArrayList<>(this.sampleCallIds);
        for (UUID id : additionalCallIds) {
            if (!merged.contains(id)) {
                merged.add(id);
            }
        }
        // 근거 콜 목록이 무한정 커지지 않게 상한을 둔다 — faq_generate.py의 MAX_CASES(20)와 같은 이유.
        // 오래된 것부터 버리고 최근 근거를 우선한다.
        int cap = 50;
        this.sampleCallIds = merged.size() > cap ? merged.subList(merged.size() - cap, merged.size()) : merged;
    }
}
