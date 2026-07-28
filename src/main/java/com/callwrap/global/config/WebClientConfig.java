package com.callwrap.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * FastAPI 호출 전용 WebClient. 나머지 앱은 서블릿(MVC)로 두고, 이 빈만 논블로킹 클라이언트로
 * 외부 HTTP 호출을 담당한다 — AiServiceClient에서 .block()으로 결과를 받아 기존 MVC 흐름에 맞춘다.
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final AiServiceProperties aiServiceProperties;

    @Bean
    public WebClient aiServiceWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, aiServiceProperties.connectTimeoutMs())
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(aiServiceProperties.readTimeoutMs(), TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(aiServiceProperties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
