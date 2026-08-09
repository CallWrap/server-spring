package com.callwrap.audio.service;

import com.callwrap.audio.config.AudioStorageProperties;
import com.callwrap.audio.dto.AudioUploadUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/*
 * 실제 업로드(바이트 전송)는 이 서비스를 거치지 않는다 — 발급받은 URL로 클라이언트가 직접
 * MinIO/S3에 PUT한다. Spring 서버 자원(메모리·네트워크)에 대용량 바이너리가 안 지나간다.
 */

@Service
@RequiredArgsConstructor
public class AudioUploadService {

    private final S3Presigner s3Presigner;
    private final AudioStorageProperties properties;

    public AudioUploadUrlResponse issueUploadUrl(String fileExtension) {
        String key = buildObjectKey(fileExtension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(properties.bucket())
            .key(key)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(properties.presignExpirationSeconds()))
            .putObjectRequest(putObjectRequest)
            .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        String audioUri = "s3://" + properties.bucket() + "/" + key;

        return new AudioUploadUrlResponse(presigned.url().toString(), audioUri);
    }

    // 날짜별 폴더로 나눠서 한 폴더에 파일이 무한정 쌓이는 걸 방지
    private String buildObjectKey(String fileExtension) {
        LocalDate today = LocalDate.now();
        String ext = (fileExtension == null || fileExtension.isBlank()) ? "wav" : fileExtension;
        return "%04d/%02d/%02d/%s.%s".formatted(
            today.getYear(), today.getMonthValue(), today.getDayOfMonth(), UUID.randomUUID(), ext);
    }
}
