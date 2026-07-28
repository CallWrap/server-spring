package com.callwrap.review.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewSummaryResponse(
        UUID reviewId,
        String status,
        int retryCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
