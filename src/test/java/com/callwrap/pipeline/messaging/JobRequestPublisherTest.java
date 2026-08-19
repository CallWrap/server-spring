package com.callwrap.pipeline.messaging;

import com.callwrap.pipeline.messaging.validation.JobRequestSchemaViolationException;
import com.callwrap.pipeline.messaging.validation.MessageSchemaValidator;
import com.callwrap.pipeline.metrics.PipelineMetrics;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * ResultMessageListenerTest와 같은 이유로 실제 MessageSchemaValidator를 쓴다 —
 * "발행 직전에 계약을 어긴 JobRequest를 실제로 막는가"를 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class JobRequestPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private PipelineMetrics pipelineMetrics;

    private JobRequestPublisher publisher;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // Instant를 숫자 타임스탬프가 아니라 ISO-8601 문자열로 직렬화한다 — 실제 빈(rabbitObjectMapper)이
            // Jackson2ObjectMapperBuilder를 통해 얻는 Spring Boot 기본 설정과 맞춰야 한다. 이게 없으면
            // job-request.schema.json의 requested_at(type: string, format: date-time) 검증이
            // "유효한" JobRequest에서도 깨진다 — 실제로 이 버그로 테스트가 한 번 실패했다.
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MessageSchemaValidator schemaValidator = new MessageSchemaValidator(mapper);
        publisher = new JobRequestPublisher(rabbitTemplate, schemaValidator, pipelineMetrics);
    }

    @Test
    void 유효한_요청이면_발행하고_카운터를_올린다() {
        publisher.publish(UUID.randomUUID(), "s3://callwrap-audio/a.wav", "wav", Map.of());

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.CALLWRAP_EXCHANGE), eq(RabbitMQConfig.JOB_REQUEST_ROUTING_KEY), any(JobRequest.class));
        verify(pipelineMetrics).recordJobPublished();
    }

    @Test
    void audio_uri가_비어있으면_발행하지_않고_예외를_던진다() {
        // job-request.schema.json: audio_uri는 minLength 1
        assertThatThrownBy(() -> publisher.publish(UUID.randomUUID(), "", "wav", Map.of()))
            .isInstanceOf(JobRequestSchemaViolationException.class);

        verify(pipelineMetrics).recordJobSchemaViolation();
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(JobRequest.class));
    }
}
