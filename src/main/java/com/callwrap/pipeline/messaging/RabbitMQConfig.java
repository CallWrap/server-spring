package com.callwrap.pipeline.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * 신규 음성 도착 이벤트 큐. 진입점(업로드 시점)만 큐잉하고,
 * 그 뒤 STT->화자분리->품질게이트->분류->요약/후속처리는 컨슈머 안에서 동기 호출 체인으로 처리한다
 * — 단계별 재큐잉은 장애 격리엔 유리하지만 지금 단계에서는 과한 복잡도를 가진다고 판단했다.
 * 실패 메시지는 DLQ로 보내 유실 없이 확인할 수 있게 한다 (재시도/실패 정책 태스크).
 */
@Configuration
public class RabbitMQConfig {

    public static final String VOICE_PROCESSING_EXCHANGE = "callwrap.voice-processing.exchange";
    public static final String VOICE_PROCESSING_QUEUE = "callwrap.voice-processing.queue";
    public static final String VOICE_PROCESSING_ROUTING_KEY = "voice-processing";

    public static final String VOICE_PROCESSING_DLX = "callwrap.voice-processing.dlx";
    public static final String VOICE_PROCESSING_DLQ = "callwrap.voice-processing.dlq";

    @Bean
    public DirectExchange voiceProcessingExchange() {
        return new DirectExchange(VOICE_PROCESSING_EXCHANGE);
    }

    @Bean
    public Queue voiceProcessingQueue() {
        return QueueBuilder.durable(VOICE_PROCESSING_QUEUE)
                .withArgument("x-dead-letter-exchange", VOICE_PROCESSING_DLX)
                .withArgument("x-dead-letter-routing-key", VOICE_PROCESSING_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding voiceProcessingBinding() {
        return BindingBuilder.bind(voiceProcessingQueue())
                .to(voiceProcessingExchange())
                .with(VOICE_PROCESSING_ROUTING_KEY);
    }

    @Bean
    public DirectExchange voiceProcessingDlx() {
        return new DirectExchange(VOICE_PROCESSING_DLX);
    }

    @Bean
    public Queue voiceProcessingDlq() {
        return QueueBuilder.durable(VOICE_PROCESSING_DLQ).build();
    }

    @Bean
    public Binding voiceProcessingDlqBinding() {
        return BindingBuilder.bind(voiceProcessingDlq())
                .to(voiceProcessingDlx())
                .with(VOICE_PROCESSING_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
