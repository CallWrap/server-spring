package com.callwrap.pipeline.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

//JobRequest 발행을 하는 곳

@Component
@RequiredArgsConstructor
public class JobRequestPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(UUID callId, String audioUri, String audioFormat, Map<String, Object> metadata) {
        JobRequest jobRequest = JobRequest.of(callId, audioUri, audioFormat, metadata);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CALLWRAP_EXCHANGE,
                RabbitMQConfig.JOB_REQUEST_ROUTING_KEY,
                jobRequest
        );
    }
}