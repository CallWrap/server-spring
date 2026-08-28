package com.callwrap.counselor.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CounselorDetailResponse(
    UUID id,
    String name,
    String team,
    long totalCalls,
    long totalCallSec,
    GradeCounts gradeCounts,
    List<SentimentPoint> sentimentTrend,
    String state,
    String stateReason
) {
    public record GradeCounts(long malicious, long normal, long praise) {
    }

    public record SentimentPoint(LocalDate date, double negativeScore) {
    }
}
