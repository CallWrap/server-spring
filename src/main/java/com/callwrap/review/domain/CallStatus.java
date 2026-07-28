package com.callwrap.review.domain;

/*
 * 파이프라인 진행 상태 FSM. DB 컬럼(calls.status)은 VARCHAR로 저장한다
 * 상태가 늘어날 때마다 DB 마이그레이션이 필요 없도록 하기 위함
 * (@Enumerated(STRING)은 컬럼 타입에 제약을 걸지 않으므로 안전).
 * UPLOADED -> TRANSCRIBING -> QUALITY_CHECK -> PASSED -> ANALYZING -> DONE
 * 분기: RETRYING(재처리) / NEEDS_HUMAN_REVIEW(사람 검토) / FAILED(실패) / CONFIRMED(상담사 확정 완료)
 */
public enum CallStatus {
    UPLOADED,
    TRANSCRIBING,
    QUALITY_CHECK,
    RETRYING,
    NEEDS_HUMAN_REVIEW,
    PASSED,
    ANALYZING,
    DONE,
    CONFIRMED,
    FAILED;

    public boolean isTerminal() {
        return this == CONFIRMED || this == FAILED;
    }
}
