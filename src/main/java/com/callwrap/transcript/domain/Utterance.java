package com.callwrap.transcript.domain;

/*
 * 품질 게이트 4개 체크가 전부 이 하나의 구조만 보고 계산 가능하도록 설계된 입력 단위
 * 이노하이 STT로 교체돼도 게이트·분석 쪽은 수정하지 않아도 된다
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
