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
    Integer callDurationSec,

    @Schema(description = "민원인 이름 — 없을 수 있음(백엔드_요청사항_상담사화면.md 1번)")
    String complainantName,

    @Schema(description = "민원인 전화번호 — 있으면 이 값으로 동일인을 판별해 상담 이력을 잇는다")
    String complainantPhone,

    @Schema(description = "민원인 소속 기관")
    String complainantOrg
) {
}
