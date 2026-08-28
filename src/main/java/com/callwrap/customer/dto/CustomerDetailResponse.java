package com.callwrap.customer.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CustomerDetailResponse(
    UUID id,
    String name,
    String phone,
    String org,
    LocalDateTime firstCalledAt,
    GradeCounts gradeCounts,
    List<MonthlyCount> monthlyCounts,
    List<LabelCount> topSubcategories
) {
    public record GradeCounts(long malicious, long normal, long praise) {
    }

    public record MonthlyCount(String month, long total, long malicious, long praise) {
    }

    public record LabelCount(String label, long count) {
    }
}
