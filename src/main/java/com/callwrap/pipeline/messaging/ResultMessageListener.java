package com.callwrap.pipeline.messaging;

import com.callwrap.pipeline.messaging.validation.MessageSchemaValidator;
import com.callwrap.pipeline.metrics.PipelineMetrics;
import com.callwrap.pipeline.service.ResultProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResultMessageListener {

    private final ResultProcessingService resultProcessingService;
    private final MessageSchemaValidator schemaValidator;
    private final PipelineMetrics pipelineMetrics;

    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    public void onResult(Message rawMessage) {
        JsonNode node;
        try {
            node = schemaValidator.parse(rawMessage.getBody());
        } catch (Exception e) {
            pipelineMetrics.recordResultSchemaViolation("malformed_json");
            log.error("[pipeline] ResultMessage JSON 파싱 실패: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException("ResultMessage가 JSON이 아님", e);
        }

        Set<ValidationMessage> violations = schemaValidator.validateResultMessage(node);
        if (!violations.isEmpty()) {
            pipelineMetrics.recordResultSchemaViolation("schema");
            log.error("[pipeline] ResultMessage 스키마 검증 실패: {}", violations);
            throw new AmqpRejectAndDontRequeueException("ResultMessage 스키마 위반: " + violations);
        }

        ResultMessage resultMessage = schemaValidator.toResultMessage(node);
        log.info("[pipeline] 결과 수신 callId={} status={}", resultMessage.callId(), resultMessage.status());
        resultProcessingService.process(resultMessage);
    }
}
