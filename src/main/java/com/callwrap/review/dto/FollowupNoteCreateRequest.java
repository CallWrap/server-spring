package com.callwrap.review.dto;

import jakarta.validation.constraints.NotBlank;

public record FollowupNoteCreateRequest(
    @NotBlank String content
) {
}
