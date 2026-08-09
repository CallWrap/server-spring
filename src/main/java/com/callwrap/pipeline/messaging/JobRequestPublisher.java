package com.callwrap.pipeline.messaging;

import com.callwrap.pipeline.messaging.validation.MessageSchemaValidator;
import com.callwrap.pipeline.messaging.validation.JobRequestSchemaViolationException;
import com.callwrap.pipeline.metrics.PipelineMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobRequestPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MessageSchemaValidator schemaValidator;
    private final PipelineMetrics pipelineMetrics;

    public void publish(UUID callId, String audioUri, String audioFormat, Map<String, Object> metadata) {
        JobRequest jobRequest = JobRequest.of(callId, audioUri, audioFormat, metadata);

        JsonNode node = schemaValidator.toJsonNode(jobRequest);
        Set<ValidationMessage> violations = schemaValidator.validateJobRequest(node);
        if (!violations.isEmpty()) {
            pipelineMetrics.recordJobSchemaViolation();
            log.error("[pipeline] JobRequest 스키마 위반 callId={} violations={}", callId, violations);
            throw new JobRequestSchemaViolationException(callId, violations);
        }

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.CALLWRAP_EXCHANGE,
            RabbitMQConfig.JOB_REQUEST_ROUTING_KEY,
            jobRequest
        );
        pipelineMetrics.recordJobPublished();
    }
}
