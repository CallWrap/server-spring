package com.callwrap.review.domain;

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

import java.time.LocalDateTime;
import java.util.UUID;

/*
 * 후속 처리 메모 한 줄(백엔드_요청사항_상담사화면.md 4번). transcripts처럼 append-only(작성만
 * 하고 쌓아두는 로그, 수정/삭제 없음)라 BaseTimeEntity(updated_at)를 상속하지 않는다 — Call의
 * 클래스 주석에 있는 같은 관례를 따른다.
 */
@Entity
@Table(name = "followup_notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FollowupNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "call_id", nullable = false)
    private UUID callId;

    /** 작성자. 후속 처리는 여러 상담사가 이어받아 처리할 수 있어(콜 담당자와 다를 수 있음) 제한을 안 둔다. */
    @Column(name = "counselor_id", nullable = false)
    private UUID counselorId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private FollowupNote(UUID callId, UUID counselorId, String content) {
        this.callId = callId;
        this.counselorId = counselorId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}
