package com.callwrap.global.common;

import java.util.List;

/**
 * AI가 내놓은 판정 하나를 표현하는 공통 구조 — {value, confidence, evidenceIndices}.
 * analysis_results의 summary/consultation_type/consultation_category/consultation_subcategory/grade/
 * needs_followup/needs_faq 컬럼이 전부 이 형태로 JSONB에 저장된다.
 * <p>
 * value의 실제 타입(문자열, boolean, 객체)은 필드마다 다르므로 Object로 열어두고, JSONB의
 * 스키마 유연성을 그대로 Java 쪽까지 가져온다. AI 쪽 라벨 체계(상담유형·분류 등)가 아직
 * 기관 확인 대기 중이라 여기서 강타입을 걸면 라벨이 바뀔 때마다 마이그레이션이 필요해진다.
 */
public record Judgment<T>(
        T value,
        Double confidence,
        List<Integer> evidenceIndices
) {
    public static <T> Judgment<T> of(T value, Double confidence, List<Integer> evidenceIndices) {
        return new Judgment<>(value, confidence, evidenceIndices);
    }
}
