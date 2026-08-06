package com.callwrap.pipeline.messaging;

import com.callwrap.global.common.Judgment;

// AI 라벨 체계 따라 필드 변경이 필요가 없도록 이렇게 구현하였습니다.

public record AnalysisPayload(
        Judgment<Object> summary,
        Judgment<Object> consultationType,
        Judgment<Object> consultationCategory,
        Judgment<Object> consultationSubcategory,
        Judgment<Object> grade,
        Judgment<Object> needsFollowup,
        Judgment<Object> needsFaq
) {
}