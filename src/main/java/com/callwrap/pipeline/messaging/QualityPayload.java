package com.callwrap.pipeline.messaging;

import java.util.List;

public record QualityPayload(
        boolean passed,
        List<String> failedChecks
) {
}