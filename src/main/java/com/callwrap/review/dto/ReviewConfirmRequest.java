package com.callwrap.review.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record ReviewConfirmRequest(
        @NotNull Map<String, Object> confirmedFields,
        List<String> editedFields,
        Integer reviewSec
) {
}
