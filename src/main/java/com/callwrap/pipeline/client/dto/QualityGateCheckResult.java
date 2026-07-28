package com.callwrap.pipeline.client.dto;

import java.util.Map;

public record QualityGateCheckResult(
        String checkName,
        boolean passed,
        double score,
        Double threshold,
        Boolean higherIsBetter,
        String action,
        String reason,
        Map<String, Object> details
) {
}
