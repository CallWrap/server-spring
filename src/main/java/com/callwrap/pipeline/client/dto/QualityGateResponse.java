package com.callwrap.pipeline.client.dto;

import java.util.List;

public record QualityGateResponse(String action, List<QualityGateCheckResult> results) {
}
