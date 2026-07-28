package com.callwrap.pipeline.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VoiceProcessingProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publish(UUID callId) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.VOICE_PROCESSING_EXCHANGE,
                RabbitMQConfig.VOICE_PROCESSING_ROUTING_KEY,
                new VoiceProcessingMessage(callId)
        );
    }
}
