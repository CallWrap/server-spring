package com.callwrap.review.dto;

import java.util.Map;

/**
 * 관리자 대시보드 개요 탭용 집계. 전부 DB에서 GROUP BY로 계산해서 내려준다 —
 * 목록을 캡(size=100) 걸어서 받아온 뒤 프론트에서 세던 방식(부정확 + 낭비)을 대체한다.
 */
public record ReviewStatsResponse(
    long totalCalls,
    Map<String, Long> statusCounts,
    Map<String, Long> gradeCounts,
    Map<String, Long> typeCounts,
    Double avgReviewSec
) {
}
