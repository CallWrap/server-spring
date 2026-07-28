package com.callwrap.counselor.service;

/*
 * 로그인 시 발급되는 토큰 쌍. accessToken은 응답 바디로, refreshToken은 HttpOnly 쿠키로 내려간다 (컨트롤러에서 분리)
 */
public record TokenPair(String accessToken, String refreshToken, long accessTokenExpiresIn) {
}
