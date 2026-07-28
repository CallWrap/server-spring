package com.callwrap.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    DUPLICATE_SOURCE_REQUEST(HttpStatus.CONFLICT, "이미 접수된 콜입니다 (sourceRequestId 중복)."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 검토입니다."),
    REVIEW_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정된 검토입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "인증이 만료되었거나 유효하지 않습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
