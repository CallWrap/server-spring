package com.callwrap.analysis.domain;

/*
 * 분석 파이프라인 중 일부 단계 (분류/요약/후속처리 등)가 실패했을 때 남기는 기록.
 * 한 단계가 실패해도 나머지는 계속 진행한다 - 나중에 상담사 대시보드에 이거 사용하기
 */
public record PipelineFailure(String stage, String reason) {
}
