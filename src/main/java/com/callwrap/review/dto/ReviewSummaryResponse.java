package com.callwrap.review.dto;

import com.callwrap.global.common.Judgment;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewSummaryResponse(
    UUID reviewId,
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
