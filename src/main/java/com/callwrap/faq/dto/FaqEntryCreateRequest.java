package com.callwrap.faq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** FastAPI가 EXAONE 배치 생성 결과를 넘길 때 쓰는 요청. */
public record FaqEntryCreateRequest(
    @NotBlank String subcategory,
    @NotBlank String question,
    @NotBlank String answer,
    @NotNull Integer sourceCallCount,
    @NotEmpty List<UUID> sampleCallIds
) {
}
