package com.callwrap.counselor.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CounselorCallResponse(
    UUID reviewId,
    int callNo,
    LocalDateTime calledAt,
    String customerName,
    String grade,
    String summary,
    Integer callDurationSec,
    Integer reviewSec,
    double negativeScore
) {
}
