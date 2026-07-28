package com.callwrap.pipeline.messaging;

import java.util.UUID;

public record VoiceProcessingMessage(UUID callId) {
}
