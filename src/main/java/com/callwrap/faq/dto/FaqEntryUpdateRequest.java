package com.callwrap.faq.dto;

import jakarta.validation.constraints.NotBlank;

/** 검수 중 상담사가 EXAONE 초안을 다듬을 때 쓴다. */
public record FaqEntryUpdateRequest(
    @NotBlank String question,
    @NotBlank String answer
) {
}
