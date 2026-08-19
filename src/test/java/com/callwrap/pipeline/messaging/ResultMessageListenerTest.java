package com.callwrap.pipeline.messaging;

import com.callwrap.pipeline.messaging.validation.MessageSchemaValidator;
import com.callwrap.pipeline.metrics.PipelineMetrics;
import com.callwrap.pipeline.service.ResultProcessingService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 실제 MessageSchemaValidator(진짜 schema/result-message.schema.json)를 그대로 쓰고,
 * resultProcessingService·pipelineMetrics만 목으로 대체한다 — "계약을 어긴 메시지가 실제로
 * 걸러지는가"를 확인하는 게 목적이라 검증 로직 자체를 목으로 바꾸면 의미가 없다.
 */
@ExtendWith(MockitoExtension.class)
class ResultMessageListenerTest {

    @Mock
    private ResultProcessingService resultProcessingService;
    @Mock
    private PipelineMetrics pipelineMetrics;

    private ResultMessageListener listener;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MessageSchemaValidator schemaValidator = new MessageSchemaValidator(mapper);
        listener = new ResultMessageListener(resultProcessingService, schemaValidator, pipelineMetrics);
    }

    @Test
    void 유효한_DONE_메시지는_그대로_처리된다() throws IOException {
        Message message = fixtureMessage("result-message-done-valid.json");

        listener.onResult(message);

        verify(resultProcessingService).process(any());
    }

    @Test
    void JSON이_아니면_DLQ로_보내고_처리하지_않는다() {
        Message message = MessageBuilder.withBody("이건 JSON이 아니다".getBytes(StandardCharsets.UTF_8)).build();

        assertThatThrownBy(() -> listener.onResult(message))
            .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(pipelineMetrics).recordResultSchemaViolation("malformed_json");
        verify(resultProcessingService, never()).process(any());
    }

    @Test
    void DONE인데_analysis가_없으면_스키마_위반으로_DLQ_보낸다() throws IOException {
        Message message = fixtureMessage("result-message-done-missing-analysis.json");

        assertThatThrownBy(() -> listener.onResult(message))
            .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(pipelineMetrics).recordResultSchemaViolation("schema");
        verify(resultProcessingService, never()).process(any());
    }

    @Test
    void FAILED인데_error가_없으면_스키마_위반으로_DLQ_보낸다() throws IOException {
        Message message = fixtureMessage("result-message-failed-missing-error.json");

        assertThatThrownBy(() -> listener.onResult(message))
            .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(pipelineMetrics).recordResultSchemaViolation("schema");
        verify(resultProcessingService, never()).process(any());
    }

    @Test
    void 존재하지_않는_콜의_결과는_DLQ로_보낸다() throws IOException {
        Message message = fixtureMessage("result-message-done-valid.json");
        doThrow(new NoSuchElementException("존재하지 않는 콜: " + UUID.randomUUID()))
            .when(resultProcessingService).process(any());

        assertThatThrownBy(() -> listener.onResult(message))
            .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    private Message fixtureMessage(String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(new ClassPathResource("fixtures/" + fileName).getFile().toPath());
        return MessageBuilder.withBody(bytes).build();
    }
}
