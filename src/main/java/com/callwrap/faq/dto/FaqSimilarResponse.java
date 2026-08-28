package com.callwrap.faq.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FaqSimilarResponse(
    UUID id,
    String question,
    String subcategory,
    String status,
    LocalDateTime updatedAt,
    double similarity
) {
}
