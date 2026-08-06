package com.callwrap.pipeline.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResultMessage(
        String schemaVersion,
        UUID callId,
        ResultStatus status,
        AnalysisPayload analysis,
        QualityPayload quality,
        List<String> failures,
        String error,
        Instant completedAt
) {
}