package com.callwrap.audio.service;

import com.callwrap.audio.config.AudioStorageProperties;
import com.callwrap.audio.dto.AudioUploadUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/*
 * 실제 업로드(바이트 전송)는 이 서비스를 거치지 않는다 — 발급받은 URL로 클라이언트가 직접
 * MinIO/S3에 PUT한다. Spring 서버 자원(메모리·네트워크)에 대용량 바이너리가 안 지나간다.
 */

@Slf4j
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

    /*
     * Call.audioPath에는 "s3://bucket/key" 형태의 내부 참조만 저장돼 있다 — 브라우저 <audio>가
     * 바로 재생할 수 없다. 재생 시점마다 짧게 살아있는 프리사인 GET URL로 바꿔서 내려준다
     * (PUT 업로드 때 쓰던 S3Presigner/AudioStorageProperties를 그대로 재사용).
     */
    public String presignPlaybackUrl(String audioUri) {
        if (audioUri == null || audioUri.isBlank() || !audioUri.startsWith("s3://")) {
            log.warn("[audio] 재생 URL 생성 불가 — audioPath가 비어있거나 s3:// 형식이 아님: {}", audioUri);
            return null;
        }

        String withoutScheme = audioUri.substring("s3://".length());
        int slashIndex = withoutScheme.indexOf('/');
        if (slashIndex < 0 || slashIndex == withoutScheme.length() - 1) {
            log.warn("[audio] 재생 URL 생성 불가 — audioPath 형식이 올바르지 않음(bucket/key 파싱 실패): {}", audioUri);
            return null;
        }

        String bucket = withoutScheme.substring(0, slashIndex);
        String key = withoutScheme.substring(slashIndex + 1);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(properties.presignExpirationSeconds()))
            .getObjectRequest(getObjectRequest)
            .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }
}
