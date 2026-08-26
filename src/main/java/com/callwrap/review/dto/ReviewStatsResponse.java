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
    Double avgReviewSec,
    Map<Integer, Long> hourlyThroughput,
    Map<String, Long> maliciousByCounselor,
    /** 판정 5종(유형·분류·등급·요약·후속필요) confidence 평균, 0~1. 판정이 하나도 없으면 null. */
    Double avgConfidence,
    /** 위 판정들 중 고신뢰(confidence >= 0.9) 비율, 0~1. 판정이 하나도 없으면 null. */
    Double highConfidenceRate
) {
}
