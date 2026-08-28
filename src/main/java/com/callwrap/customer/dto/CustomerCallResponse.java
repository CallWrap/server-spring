package com.callwrap.customer.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerCallResponse(
    UUID reviewId,
    int callNo,
    LocalDateTime calledAt,
    String counselorName,
    String grade,
    String summary,
    Integer callDurationSec,
    Integer reviewSec
) {
}
