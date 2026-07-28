package com.callwrap.pipeline.messaging;

import com.callwrap.pipeline.service.PipelineOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceProcessingConsumer {

    private final PipelineOrchestrationService orchestrationService;

    @RabbitListener(queues = RabbitMQConfig.VOICE_PROCESSING_QUEUE)
    public void onMessage(VoiceProcessingMessage message) {
        log.info("[pipeline] 콜 처리 시작 callId={}", message.callId());
        orchestrationService.process(message.callId());
    }
}
