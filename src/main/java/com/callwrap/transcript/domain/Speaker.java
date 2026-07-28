package com.callwrap.transcript.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/*
 * ai/schemas.py의 Speaker(COUNSELOR="상담사", CUSTOMER="고객")와 값이 동일해야 한다.
 * FastAPI가 내려주는 JSON의 speaker 문자열을 그대로 이 enum으로 역직렬화한다
 */
public enum Speaker {
    COUNSELOR("상담사"),
    CUSTOMER("고객");

    private final String label;

    Speaker(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static Speaker fromLabel(String label) {
        for (Speaker speaker : values()) {
            if (speaker.label.equals(label)) {
                return speaker;
            }
        }
        throw new IllegalArgumentException("알 수 없는 화자 값: " + label);
    }
}
