package com.callwrap.pipeline.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record JobRequest(
        String schemaVersion,
        UUID callId,
        String audioUri,
        String audioFormat,
        Map<String, Object> metadata,
        Instant requestedAt
) {
    private static final String CURRENT_SCHEMA_VERSION = "1.0";

    public static JobRequest of(UUID callId, String audioUri, String audioFormat, Map<String, Object> metadata) {
        return new JobRequest(CURRENT_SCHEMA_VERSION, callId, audioUri, audioFormat, metadata, Instant.now());
    }
}