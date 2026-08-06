package com.callwrap.pipeline.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CALLWRAP_EXCHANGE = "callwrap";
    public static final String JOB_REQUEST_ROUTING_KEY = "call.analyze.request";
    public static final String RESULT_ROUTING_KEY = "call.analyze.result";

    public static final String RESULT_QUEUE = "callwrap.spring.result-queue";

    @Bean
    public TopicExchange callwrapExchange() {
        return new TopicExchange(CALLWRAP_EXCHANGE, true, false);
    }

    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE).build();
    }

    @Bean
    public Binding resultBinding() {
        return BindingBuilder.bind(resultQueue())
            .to(callwrapExchange())
            .with(RESULT_ROUTING_KEY);
    }

    @Bean(name = "rabbitObjectMapper")
    public ObjectMapper rabbitObjectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Bean
    public MessageConverter jsonMessageConverter(@Qualifier("rabbitObjectMapper") ObjectMapper rabbitObjectMapper) {
        return new Jackson2JsonMessageConverter(rabbitObjectMapper);
    }
}
