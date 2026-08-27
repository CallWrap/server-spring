package com.callwrap.global.config;

import com.callwrap.counselor.domain.CounselorRole;
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
                // 컨테이너가 오류를 /error로 포워드하는데 여기가 막혀 있으면 실제 상태코드(400/404 등)가
                // 전부 403으로 뒤바뀐다 — 원인을 못 찾게 되므로 열어둔다. 인증이 필요한 자원은
                // 어차피 각자 규칙에서 이미 막히고, /error는 그 결과만 렌더한다.
                .requestMatchers("/error").permitAll()
                // 검토 목록 실시간 갱신 스트림 — 네이티브 EventSource는 Authorization 헤더를 못 보낸다
                // (브라우저 표준 제약, 쿠키만 가능). 이 스트림이 보내는 데이터는 {callId, status}뿐이라
                // 새어나갈 내용이 없어 permitAll로 둔다.
                // 주의: 목록 조회(ReviewQueryService.list())는 이제 상담사별로 필터링되지만 이 스트림은
                // 그렇지 않다 — 다른 상담사의 콜 이벤트도 전원에게 간다. 받는 쪽이 목록을 다시 조회하면
                // 어차피 본인 것만 오므로 동작 문제는 없다. 여기에 다른 데이터를 싣게 되면 그때는
                // 쿠키 기반 인증으로 바꿔야 한다.
                .requestMatchers(HttpMethod.GET, "/api/reviews/stream").permitAll()
                // 사보원 콜 접수 시스템 전용
                .requestMatchers(HttpMethod.POST, "/api/reviews", "/api/audio/upload-url")
                    .hasAuthority(ApiKeyAuthFilter.SCOPE_INTAKE)
                // FastAPI 배치 생성기 전용
                .requestMatchers("/api/internal/faq/**").hasAuthority(ApiKeyAuthFilter.SCOPE_INTERNAL)
                // 관리자 전용 — FAQ 검수 화면, 대시보드 집계(개요 탭). 상담사는 접근 자체가 안 되게 막는다.
                .requestMatchers("/api/faq-entries/**").hasAuthority(CounselorRole.ADMIN.name())
                .requestMatchers(HttpMethod.GET, "/api/reviews/stats").hasAuthority(CounselorRole.ADMIN.name())
                // 상담사 전용 — 콜 검토 확정. 관리자는 검토 업무를 하지 않는다.
                .requestMatchers(HttpMethod.PATCH, "/api/reviews/*/confirm").hasAuthority(CounselorRole.COUNSELOR.name())
                // 나머지(대기열 조회 등)는 역할 상관없이 로그인한 사람이면 됨 (JWT)
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
