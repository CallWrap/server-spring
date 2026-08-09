package com.callwrap.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record ReviewCreateRequest(
    @Schema(description = "사보원 측 중복 방지용 요청 식별자", example = "REQ-2026-000123")
    @NotBlank String sourceRequestId,

    @Schema(description = "상담사 UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    @NotBlank String counselorId,

    @Schema(description = "음성 파일이 저장된 위치의 URL/경로")
    @NotBlank String audioFileUrl,

    @Schema(description = "통화 시작 시각")
    LocalDateTime callStartedAt,

    @Schema(description = "통화 시간(초)")
    Integer callDurationSec
) {
}
