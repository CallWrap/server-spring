package com.callwrap.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * callwrap.ai-service.* 설정 — FastAPI(AI 모델 호출 레이어)의 위치와 타임아웃.
 */
@ConfigurationProperties(prefix = "callwrap.ai-service")
public record AiServiceProperties(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
