package com.callwrap.review.dto;

import com.callwrap.global.common.Judgment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReviewDetailResponse(
    UUID reviewId,
    String status,
    LocalDateTime callStartedAt,
    Integer callDurationSec,
    LocalDateTime createdAt,
    ClassificationView classification,
    Judgment<Object> summary,
    Judgment<Object> needsFollowup,
    Judgment<Object> needsFaq,
    List<String> failures,
    LocalDateTime confirmedAt,
    UUID confirmedByCounselorId
) {
    public record ClassificationView(
        Judgment<Object> type,
        Judgment<Object> category,
        Judgment<Object> subcategory,
        Judgment<Object> grade
    ) {
    }
}
