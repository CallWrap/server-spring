package com.callwrap.counselor.controller;

import com.callwrap.counselor.dto.LoginRequest;
import com.callwrap.counselor.dto.LoginResponse;
import com.callwrap.counselor.dto.TokenRefreshResponse;
import com.callwrap.counselor.service.AuthService;
import com.callwrap.counselor.service.TokenPair;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenPair tokenPair = authService.loginAndIssueTokens(request);
        ResponseCookie cookie = refreshTokenCookie(tokenPair.refreshToken(), Duration.ofDays(14));
        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(new LoginResponse(tokenPair.accessToken(), tokenPair.accessTokenExpiresIn()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(HttpServletRequest request) {
        String refreshToken = extractRefreshToken(request);
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie expired = refreshTokenCookie("", Duration.ZERO);
        return ResponseEntity.noContent().header("Set-Cookie", expired.toString()).build();
    }

    private ResponseCookie refreshTokenCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        throw new CallWrapException(ErrorCode.INVALID_TOKEN);
    }
}
