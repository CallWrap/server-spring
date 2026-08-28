package com.callwrap.faq.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FaqRevisionResponse(
    UUID id,
    String question,
    String answer,
    UUID revisedByCounselorId,
    LocalDateTime revisedAt
) {
}
