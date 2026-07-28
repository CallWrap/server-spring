package com.callwrap.transcript.domain;

/**
 * 품질 게이트 4개 체크가 전부 이 하나의 구조만 보고 계산 가능하도록 설계된 입력 단위
 * (기존 ai/schemas.py Transcript/Utterance와 동일한 계약). 이노하이 STT로 교체돼도
 * 이 구조만 유지되면 게이트·분석 쪽은 무수정이다.
 */
public record Utterance(
        int index,
        Speaker speaker,
        String text,
        double start,
        double end,
        Double confidence
) {
}
