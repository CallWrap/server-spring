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

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/*
 * RabbitMQ는 큐 적재량을 push로 알려주지 않아서, Management HTTP API를 주기적으로 폴링해서 Gauge를 갱신하는 방식으로 구현했다.
 * 폴링 실패가 서비스 흐름에 영향을 주면 안 되므로 예외는 로그로만 남기고 넘어간다.
 *
 * result-queue(FastAPI→Spring)뿐 아니라 jobs 큐(Spring→FastAPI, FastAPI가 소비)도 같이 본다 —
 * 부하가 몰렸을 때 실제로 먼저 쌓이는 쪽은 FastAPI 워커가 못 따라가는 jobs 큐라서, 여기가 안 보이면
 * "느려졌다"는 알아도 "어디서 느려졌다"는 알 수 없다.
 * JOBS_QUEUE 문자열은 Spring이 직접 선언/소비하는 큐가 아니라 모니터링 목적으로만 참조하는 이름이라
 * FastAPI messaging/topology.py의 QUEUE_JOBS 상수와 항상 같은 값으로 맞춰둬야 한다.
 */

@Slf4j
@Component
public class QueueDepthMetrics {

    private static final String JOBS_QUEUE = "callwrap.jobs";

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, AtomicLong> depthByQueue = new ConcurrentHashMap<>();

    @Value("${callwrap.rabbitmq.management.base-url:http://localhost:15672}")
    private String managementBaseUrl;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.password:guest}")
    private String password;

    public QueueDepthMetrics(MeterRegistry meterRegistry) {
        registerGauge(meterRegistry, RabbitMQConfig.RESULT_QUEUE);
        registerGauge(meterRegistry, JOBS_QUEUE);
    }

    private void registerGauge(MeterRegistry meterRegistry, String queueName) {
        AtomicLong depth = depthByQueue.computeIfAbsent(queueName, q -> new AtomicLong(0));
        Gauge.builder("callwrap_queue_depth", depth, AtomicLong::get)
            .description("RabbitMQ 큐 적재 메시지 수")
            .tag("queue", queueName)
            .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 10_000)
    public void refresh() {
        depthByQueue.keySet().forEach(this::refreshQueue);
    }

    private void refreshQueue(String queueName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(username, password);
            URI uri = URI.create(managementBaseUrl + "/api/queues/%2F/" + queueName);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
            JsonNode body = response.getBody();
            if (body != null && body.has("messages")) {
                depthByQueue.get(queueName).set(body.get("messages").asLong());
            }
        } catch (Exception e) {
            log.warn("[metrics] 큐({}) 적재량 조회 실패: {}", queueName, e.getMessage());
        }
    }
}
