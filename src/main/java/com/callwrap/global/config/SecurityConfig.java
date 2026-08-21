package com.callwrap.global.config;

import com.callwrap.global.security.ApiKeyAuthFilter;
import com.callwrap.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/*
 * 서버는 STATELESS로 동작 — 세션 없이 매 요청마다 인증한다. 인증 방식은 호출 주체에 따라 둘로 나뉜다:
 *
 * - JWT (JwtAuthenticationFilter): 상담사가 로그인 후 web에서 부르는 모든 경로.
 * - API Key (ApiKeyAuthFilter): 사람이 아니라 다른 서버가 부르는 시스템 간 경로.
 *   사보원 콜 접수 시스템(POST /api/reviews, POST /api/audio/upload-url)과
 *   FastAPI 배치 생성기(/api/internal/faq/**)가 여기 해당한다.
 *
 * 두 필터 모두 "토큰/키가 있으면 SecurityContext를 채우고, 없어도 필터 자체는 막지 않는다" —
 * 실제 차단은 authorizeHttpRequests의 규칙이 한다. 필터는 인증(누구인가)만, 아래 규칙은
 * 인가(무엇을 할 수 있는가)만 맡는다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthFilter apiKeyAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 로그인 자체는 인증 이전 단계이므로 열어둔다
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                // 개발 도구·헬스체크
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                // 사보원 콜 접수 시스템 전용
                .requestMatchers(HttpMethod.POST, "/api/reviews", "/api/audio/upload-url")
                    .hasAuthority(ApiKeyAuthFilter.SCOPE_INTAKE)
                // FastAPI 배치 생성기 전용
                .requestMatchers("/api/internal/faq/**").hasAuthority(ApiKeyAuthFilter.SCOPE_INTERNAL)
                // 나머지는 로그인한 상담사만 (JWT)
                .anyRequest().authenticated())
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
