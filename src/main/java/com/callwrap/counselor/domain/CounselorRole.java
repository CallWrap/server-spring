package com.callwrap.counselor.domain;

/*
 * COUNSELOR(상담사) — 검토 대기열/후속 처리/처리 완료 화면(콜 검토·확정)만 접근.
 * ADMIN(중간 관리자) — 모니터링 화면(개요/측정/분포/상담대장/품질/검증/FAQ 검수)만 접근. 콜 검토는 하지 않는다.
 * 이 값 하나로 프론트(라우트·사이드바)와 백엔드(SecurityConfig 인가 규칙) 양쪽의 화면/API 접근을 가른다.
 */
public enum CounselorRole {
    COUNSELOR,
    ADMIN
}
