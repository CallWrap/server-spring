package com.callwrap.counselor.service;

import com.callwrap.counselor.domain.Counselor;
import com.callwrap.counselor.dto.LoginRequest;
import com.callwrap.counselor.dto.TokenRefreshResponse;
import com.callwrap.counselor.repository.CounselorRepository;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.callwrap.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 인증(로그인/토큰 재발급)만 책임진다. 상담사 조회(내 정보)는 CounselorQueryService가 별도로 맡는다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CounselorRepository counselorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public TokenPair loginAndIssueTokens(LoginRequest request) {
        Counselor counselor = counselorRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new CallWrapException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), counselor.getPasswordHash())) {
            throw new CallWrapException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(counselor.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(counselor.getId());
        return new TokenPair(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpirationSeconds());
    }

    public TokenRefreshResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CallWrapException(ErrorCode.INVALID_TOKEN);
        }
        UUID counselorId = jwtTokenProvider.getCounselorId(refreshToken);
        String accessToken = jwtTokenProvider.createAccessToken(counselorId);
        return new TokenRefreshResponse(accessToken, jwtTokenProvider.getAccessTokenExpirationSeconds());
    }

    // TODO: 로그아웃 시 refreshToken 서버측 폐기(Redis 블랙리스트). 지금은 컨트롤러에서 쿠키만 지운다.
}
