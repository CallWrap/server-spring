package com.callwrap.pipeline.messaging;

import com.callwrap.pipeline.messaging.validation.MessageSchemaValidator;
import com.callwrap.pipeline.messaging.validation.JobRequestSchemaViolationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ValidationMessage;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JobRequestPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MessageSchemaValidator schemaValidator;
    private final MeterRegistry meterRegistry;

    public void publish(UUID callId, String audioUri, String audioFormat, Map<String, Object> metadata) {
        JobRequest jobRequest = JobRequest.of(callId, audioUri, audioFormat, metadata);

        JsonNode node = schemaValidator.toJsonNode(jobRequest);
        Set<ValidationMessage> violations = schemaValidator.validateJobRequest(node);
        if (!violations.isEmpty()) {
            meterRegistry.counter("callwrap_job_schema_violation_total").increment();
            throw new JobRequestSchemaViolationException(callId, violations);
        }

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.CALLWRAP_EXCHANGE,
            RabbitMQConfig.JOB_REQUEST_ROUTING_KEY,
            jobRequest
        );
    }
}
