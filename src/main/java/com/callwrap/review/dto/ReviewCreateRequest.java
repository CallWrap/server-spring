package com.callwrap.review.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/*
 * callStartedAt은 API 계약상 받되 아직 저장 컬럼이 없다 (calls 테이블에 call_started_at 미정의,,)
 * 투두 - 필요해지면 마이그레이션 추가 — 지금은 로깅/추후 확장 용도로만 받아둔다.
 */
public record ReviewCreateRequest(
        @Schema(description = "사보원 측 중복 방지용 요청 식별자", example = "REQ-2026-000123")
        @NotBlank String sourceRequestId,

        @Schema(description = "상담사 UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotBlank String counselorId,

        @Schema(description = "음성 파일이 저장된 위치의 URL/경로")
        @NotBlank String audioFileUrl,

        @Schema(description = "통화 시작 시각 (현재 저장은 안 되고 계약상 받아만 둠)")
        LocalDateTime callStartedAt
) {
}
