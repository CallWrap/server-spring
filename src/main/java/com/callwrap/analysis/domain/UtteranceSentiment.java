package com.callwrap.analysis.domain;

/** 고객·상담사 발화 하나의 감정 점수. score 범위는 AI 계약상 -1~1이다. */
public record UtteranceSentiment(
    int utteranceIndex,
    String speaker,
    double score
) {
}
