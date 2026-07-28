package com.callwrap.global.exception;

import lombok.Getter;

@Getter
public class CallWrapException extends RuntimeException {

    private final ErrorCode errorCode;

    public CallWrapException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
