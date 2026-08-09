package com.callwrap.audio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "callwrap.audio-storage")
public record AudioStorageProperties(
    String endpoint,
    String accessKey,
    String secretKey,
    String bucket,
    long presignExpirationSeconds
) {
}
