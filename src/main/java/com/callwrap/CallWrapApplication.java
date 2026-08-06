package com.callwrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(AiServiceProperties.class)
@SpringBootApplication
public class CallWrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(CallWrapApplication.class, args);
    }
}
