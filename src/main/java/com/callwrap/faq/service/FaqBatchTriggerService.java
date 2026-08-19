package com.callwrap.faq.service;

import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/*
 * web은 Spring만 알면 되게(단일 origin, JWT 인증 하나로 통일) FastAPI의 배치 트리거를
 * 대신 호출해주는 얇은 프록시. QueueDepthMetrics의 RestTemplate+@Value 패턴을 그대로 따랐다.
 */
@Slf4j
@Service
public class FaqBatchTriggerService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${callwrap.fastapi.base-url}")
    private String fastApiBaseUrl;

    public Map<String, Object> triggerGenerateBatch() {
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                fastApiBaseUrl + "/internal/faq/generate-batch", null, JsonNode.class);
            JsonNode body = response.getBody();
            if (body == null) {
                return Map.of("groupsProcessed", 0, "entriesCreated", 0);
            }
            return Map.of(
                "groupsProcessed", body.path("groups_processed").asInt(0),
                "entriesCreated", body.path("entries_created").asInt(0)
            );
        } catch (RestClientException e) {
            log.error("[faq] FastAPI 배치 생성 트리거 실패: {}", e.getMessage());
            throw new CallWrapException(ErrorCode.FAQ_BATCH_TRIGGER_FAILED);
        }
    }
}
