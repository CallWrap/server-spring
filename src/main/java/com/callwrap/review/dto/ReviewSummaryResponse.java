package com.callwrap.review.dto;

import com.callwrap.global.common.Judgment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 대기열 한 줄. reviewId는 UUID라 사람이 알아볼 수 없어서 요약 제목을 함께 내려준다 —
 * 이게 없으면 상담사가 목록에서 어떤 콜인지 구분할 방법이 없다(분석 전이면 null).
 */
public record ReviewSummaryResponse(
    UUID reviewId,
    String title,
    String status,
    int retryCount,
    Judgment<Object> grade,
    Judgment<Object> needsFollowup,
    int highConfidenceFieldCount,
    int totalFieldCount,
    LocalDateTime callStartedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
