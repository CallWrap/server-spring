package com.callwrap.customer.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerSummaryResponse(
    UUID id,
    String name,
    String phone,
    String org,
    long totalCalls,
    long maliciousCalls,
    LocalDateTime lastCalledAt
) {
}
