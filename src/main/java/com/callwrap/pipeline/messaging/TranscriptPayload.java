package com.callwrap.pipeline.messaging;

import com.callwrap.transcript.domain.Utterance;

import java.util.List;

// FastAPI ResultMessage.transcript(Transcript.model_dump)를 그대로 받는 메시징 레이어 페이로드.
// Utterance는 domain 레코드를 그대로 재사용한다 — 순수 값객체라 여기서 새로 만들 이유가 없다.
public record TranscriptPayload(List<Utterance> utterances) {
}
