package com.callwrap.review.dto;

import java.util.List;
import java.util.Map;

/**
 * 관리자 대시보드 개요 탭용 집계. 전부 DB에서 GROUP BY로 계산해서 내려준다 —
 * 목록을 캡(size=100) 걸어서 받아온 뒤 프론트에서 세던 방식(부정확 + 낭비)을 대체한다.
 */
public record ReviewStatsResponse(
    long totalCalls,
    Map<String, Long> statusCounts,
    /**
     * 확정(CONFIRMED)된 콜을 후속 처리 필요 여부로 나눈 수 — 화면의 "진행 중"/"완료" 카운터용.
     * statusCounts의 CONFIRMED가 이 둘의 합이다.
     */
    long inProgressCount,
    long completedCount,
    Map<String, Long> gradeCounts,
    Map<String, Long> typeCounts,
    Map<String, Long> categoryCounts,
    Double avgReviewSec,
    /** 오늘의 시간대별 처리 건수를 등급으로 쪼갠 것. 시각 오름차순, 처리가 없던 시간대는 빠져 있다. */
    List<HourlyGrade> hourlyThroughput,
    Map<String, Long> maliciousByCounselor,
    /** 판정 5종(유형·분류·등급·요약·후속필요) confidence 평균, 0~1. 판정이 하나도 없으면 null. */
    Double avgConfidence,
    /** 위 판정들 중 고신뢰(confidence >= 0.9) 비율, 0~1. 판정이 하나도 없으면 null. */
    Double highConfidenceRate
) {
    /** 한 시간대의 등급별 처리 건수. total은 프론트에서 다시 세지 않도록 여기서 더해 보낸다. */
    public record HourlyGrade(int hour, long normal, long praise, long malicious) {
        public long total() {
            return normal + praise + malicious;
        }
    }
}
