package com.callwrap.review.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewConfirmResponse(
        UUID reviewId,
        String status,
        LocalDateTime confirmedAt,
        UUID confirmedByCounselorId
) {
}
