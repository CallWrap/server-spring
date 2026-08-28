package com.callwrap.counselor.dto;

import java.util.UUID;

public record CounselorSummaryResponse(
    UUID id,
    String name,
    String team,
    long totalCalls,
    long maliciousCalls,
    long avgCallSec,
    long avgReviewSec,
    String state
) {
}
