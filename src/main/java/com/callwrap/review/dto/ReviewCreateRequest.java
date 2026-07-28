package com.callwrap.review.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * callStartedAt은 API 계약상 받되 아직 저장 컬럼이 없다 (calls 테이블에 call_started_at 미정의).
 * 필요해지면 마이그레이션 추가 — 지금은 로깅/추후 확장 용도로만 받아둔다.
 */
public record ReviewCreateRequest(
        @NotBlank String sourceRequestId,
        @NotBlank String counselorId,
        @NotBlank String audioFileUrl,
        LocalDateTime callStartedAt
) {
}
