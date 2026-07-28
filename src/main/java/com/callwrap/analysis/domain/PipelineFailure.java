package com.callwrap.analysis.domain;

/**
 * 분석 파이프라인 중 일부 단계(분류/요약/후속처리 등)가 실패했을 때 남기는 기록.
 * 한 단계가 실패해도 나머지는 계속 진행한다 — 상담사에게 "이 부분만 비어있다"를 보여줄 수 있게.
 */
public record PipelineFailure(String stage, String reason) {
}
