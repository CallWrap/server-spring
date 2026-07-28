package com.callwrap.pipeline.client.dto;

import com.callwrap.transcript.domain.Utterance;

import java.util.List;

public record DiarizeResponse(List<Utterance> utterances) {
}
