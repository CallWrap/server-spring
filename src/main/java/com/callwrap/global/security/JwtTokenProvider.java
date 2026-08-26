package com.callwrap.global.security;

import com.callwrap.counselor.domain.CounselorRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public JwtTokenProvider(
            @Value("${callwrap.jwt.secret}") String secret,
            @Value("${callwrap.jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds,
            @Value("${callwrap.jwt.refresh-token-expiration-seconds}") long refreshTokenExpirationSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    // role을 토큰에 심어서, 매 요청마다 DB를 안 타고도 인가 규칙(SecurityConfig)이 검사할 수 있게 한다.
    // refreshToken에는 role을 넣지 않는다 — 재발급 시 AuthService.refresh()가 DB에서 현재 role을 다시
    // 읽어 새 accessToken에 심으므로, role이 나중에 바뀌어도 만료 전까지 옛 값이 남는 일이 없다.
    public String createAccessToken(UUID counselorId, CounselorRole role) {
        return createToken(counselorId, role, accessTokenExpirationSeconds, "access");
    }

    public String createRefreshToken(UUID counselorId) {
        return createToken(counselorId, null, refreshTokenExpirationSeconds, "refresh");
    }

    private String createToken(UUID counselorId, CounselorRole role, long expirationSeconds, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000);
        var builder = Jwts.builder()
                .subject(counselorId.toString())
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(expiry);
        if (role != null) {
            builder.claim("role", role.name());
        }
        return builder.signWith(key).compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UUID getCounselorId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }

    /** refreshToken처럼 role 클레임이 없는 토큰이면 null. */
    public CounselorRole getRole(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String role = claims.get("role", String.class);
        return role != null ? CounselorRole.valueOf(role) : null;
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }
}
