package com.callwrap.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/*
 * 시스템 간 호출(사보원 콜 접수, FastAPI 내부 API)을 인증한다. 사람이 로그인하는 게 아니라
 * "미리 발급한 비밀 값을 아는 시스템인가"만 확인하면 되므로 JWT 대신 고정 API Key를 쓴다.
 * 키 하나당 호출자 범위(scope)를 GrantedAuthority 하나로 매핑해서, SecurityConfig에서
 * "이 키는 이 경로만" 식으로 최소 권한을 강제한다 (사보원 키로 FastAPI 내부 API를 부를 수 없다).
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";
    public static final String SCOPE_INTAKE = "API_KEY_INTAKE";
    public static final String SCOPE_INTERNAL = "API_KEY_INTERNAL";

    @Value("${callwrap.api-key.intake}")
    private String intakeKey;

    @Value("${callwrap.api-key.internal}")
    private String internalKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String key = request.getHeader(HEADER);
        if (key != null) {
            String scope = resolveScope(key);
            if (scope != null) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        scope, null, List.of(new SimpleGrantedAuthority(scope)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveScope(String key) {
        if (constantTimeEquals(key, intakeKey)) return SCOPE_INTAKE;
        if (constantTimeEquals(key, internalKey)) return SCOPE_INTERNAL;
        return null;
    }

    /** String.equals는 앞에서부터 다르면 바로 끝나 미묘한 시간차(타이밍 사이드채널)가 난다.
     * 비밀값 비교는 항상 이렇게 상수 시간으로 한다. */
    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
