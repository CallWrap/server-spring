package com.callwrap.review.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewCreateResponse(UUID reviewId, String status, LocalDateTime createdAt) {
}
