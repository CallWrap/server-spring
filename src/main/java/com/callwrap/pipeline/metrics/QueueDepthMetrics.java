package com.callwrap.pipeline.metrics;

import com.callwrap.pipeline.messaging.RabbitMQConfig;
import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicLong;

/*
 * RabbitMQ는 큐 적재량을 push로 알려주지 않아서, Management HTTP API를 주기적으로 폴링해서 Gauge를 갱신하는 방식으로 구현했다.
 * 폴링 실패가 서비스 흐름에 영향을 주면 안 되므로 예외는 로그로만 남기고 넘어간다.
 */

@Slf4j
@Component
public class QueueDepthMetrics {

    private final RestTemplate restTemplate = new RestTemplate();
    private final AtomicLong resultQueueDepth = new AtomicLong(0);

    @Value("${callwrap.rabbitmq.management.base-url:http://localhost:15672}")
    private String managementBaseUrl;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.password:guest}")
    private String password;

    public QueueDepthMetrics(MeterRegistry meterRegistry) {
        Gauge.builder("callwrap_queue_depth", resultQueueDepth, AtomicLong::get)
            .description("RabbitMQ 큐 적재 메시지 수")
            .tag("queue", RabbitMQConfig.RESULT_QUEUE)
            .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 10_000)
    public void refresh() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(username, password);
            String url = managementBaseUrl + "/api/queues/%2F/" + RabbitMQConfig.RESULT_QUEUE;
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
            JsonNode body = response.getBody();
            if (body != null && body.has("messages")) {
                resultQueueDepth.set(body.get("messages").asLong());
            }
        } catch (Exception e) {
            log.warn("[metrics] RabbitMQ 큐 적재량 조회 실패: {}", e.getMessage());
        }
    }
}
