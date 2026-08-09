package com.callwrap.audio.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/*
 * MinIO는 S3 API 호환이라 endpointOverride만 걸면 AWS SDK를 그대로 쓸 수 있다.
 * pathStyleAccessEnabled(true)가 필수 — MinIO는 virtual-hosted-style(bucket.host.com)을
 * 기본 지원 안 하고 path-style(host.com/bucket)만 받는다. 나중에 진짜 S3로 옮기면
 * endpointOverride만 지우면 되고 이 클래스의 나머지는 안 바뀐다.
 */

@Configuration
@RequiredArgsConstructor
public class AudioStorageConfig {

    private final AudioStorageProperties properties;

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
            .endpointOverride(URI.create(properties.endpoint()))
            .region(Region.US_EAST_1) // MinIO는 리전 개념이 없지만 SDK가 값을 요구해서 더미로 지정
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
            .build();
    }
}
