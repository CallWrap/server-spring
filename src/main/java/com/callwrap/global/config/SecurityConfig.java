package com.callwrap.global.config;

import com.callwrap.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/*
 * 서버는 STATELESS로 동작 — 세션 없이 매 요청마다 JWT로 인증(하려고 했음).
 *
 * TODO: 시스템 간 API Key 인증(사보원->Spring)과 web 쪽 JWT 연동 검증이
 * 아직 안 끝나서, 그거 붙기 전까지는 전부 permitAll로 열어둔다. 로그인/토큰 발급
 * 자체(AuthController, JwtTokenProvider)는 그대로 동작하고, 요청에 토큰이 실려 오면
 * JwtAuthenticationFilter가 인증 컨텍스트를 채워서 @AuthenticationPrincipal도 정상
 * 동작한다 — "토큰이 있으면 쓰고, 없어도 막지 않는다"로 바뀐 것뿐.
 * 데모 끝나고 정식 인증 붙이면 anyRequest()를 authenticated()로 되돌리고, PERMIT_ALL
 * 목록을 다시 세분화할 것.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
