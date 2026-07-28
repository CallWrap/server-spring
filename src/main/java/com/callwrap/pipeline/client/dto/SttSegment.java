package com.callwrap.pipeline.client.dto;

/** POST /stt 응답 세그먼트 — 화자 정보는 아직 없다(diarize 단계에서 채워짐). */
public record SttSegment(int index, String text, double start, double end, Double confidence) {
}
