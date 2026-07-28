package com.callwrap.review.domain;

import com.callwrap.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/*
 * 통화 한 건. 파이프라인 진행 상태를 추적하는 중심 테이블
 * — transcripts/quality_check_results/analysis_results가 전부 이 테이블의 id(call_id)에 의존
 * counselorId는 연관관계 (@ManyToOne Counselor)로 두지 않고 원시 UUID로만 들고 있다.
 * 대용량 목록/상세 조회가 가장 빈번한 경로인데, 연관관계로 두면 상담사 이름이 필요 없는 화면에서도
 * 실수로 지연로딩이 터지거나 N+1이 생기기 쉽다.
 * 상담사 정보가 실제로 필요한 조회는 리포지토리에 명시적 JOIN 쿼리를 별도로 만들어 그때만 가져오도록 설계하였다.
 */
@Entity
@Table(name = "calls")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Call extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "counselor_id", nullable = false)
    private UUID counselorId;

    @Column(name = "audio_path", length = 500)
    private String audioPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CallStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "source_request_id", nullable = false, unique = true, length = 200)
    private String sourceRequestId;

    @Builder
    private Call(UUID counselorId, String audioPath, String sourceRequestId) {
        this.counselorId = counselorId;
        this.audioPath = audioPath;
        this.sourceRequestId = sourceRequestId;
        this.status = CallStatus.UPLOADED;
        this.retryCount = 0;
    }

    // 상태 전이는 이 메서드로만 — "진실의 통로를 하나로" 원칙(기존 state.py의
    // repository.set_status() 원칙을 Java 쪽으로 그대로 계승)
    public void transitionTo(CallStatus next) {
        this.status = next;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}
