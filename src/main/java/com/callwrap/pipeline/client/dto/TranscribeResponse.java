package com.callwrap.pipeline.client.dto;

import java.util.List;

public record TranscribeResponse(List<SttSegment> segments) {
}
