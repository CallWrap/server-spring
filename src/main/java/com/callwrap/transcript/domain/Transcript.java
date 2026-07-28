package com.callwrap.transcript.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/*
 * 콜 하나의 전사 결과 한 회차. call_id에 UNIQUE를 걸지 않는다 — 재시도마다 새 행을 쌓아
 * "재시도가 실제로 좋아졌는가"를 나중에 되짚을 수 있게 한다 (quality_check_results와 같은 원칙).
 * 그래서 append-only: BaseTimeEntity(updated_at)를 상속하지 않는다.!
 */
@Entity
@Table(name = "transcripts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transcript {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "call_id", nullable = false)
    private UUID callId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Utterance> utterances;

    @Column(name = "retry_iteration", nullable = false)
    private int retryIteration;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Transcript(UUID callId, List<Utterance> utterances, int retryIteration) {
        this.callId = callId;
        this.utterances = utterances;
        this.retryIteration = retryIteration;
        this.createdAt = LocalDateTime.now();
    }
}
