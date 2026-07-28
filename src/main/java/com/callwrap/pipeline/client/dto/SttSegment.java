package com.callwrap.pipeline.client.dto;

/* POST /stt 응답 세그먼트 — diarize 단계에서 화자정보가 채워짐 */
public record SttSegment(int index, String text, double start, double end, Double confidence) {
}
