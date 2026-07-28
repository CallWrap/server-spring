package com.callwrap.qualitycheck.domain;

/**
 * quality_check_results.action / FastAPI /quality-gate 응답의 action 값을 다루기 위한
 * 서비스 계층 전용 유틸 enum. DB·JSON에는 이 enum이 아니라 원시 문자열(pass/retry/human_review)이
 * 그대로 저장된다 — JPA에 매핑하지 않는다.
 */
public enum QualityAction {
    PASS("pass"),
    RETRY("retry"),
    HUMAN_REVIEW("human_review");

    private final String value;

    QualityAction(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static QualityAction fromValue(String value) {
        for (QualityAction action : values()) {
            if (action.value.equals(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("알 수 없는 action 값: " + value);
    }
}
