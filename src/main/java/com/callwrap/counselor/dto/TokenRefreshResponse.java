package com.callwrap.counselor.dto;

public record TokenRefreshResponse(String accessToken, long expiresIn) {
}
