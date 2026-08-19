package com.callwrap.faq.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FaqEntryResponse(
    UUID id,
    String subcategory,
    String question,
    String answer,
    Integer sourceCallCount,
    List<UUID> sampleCallIds,
    String status,
    LocalDateTime generatedAt,
    LocalDateTime publishedAt
) {
}
