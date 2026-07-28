package com.callwrap.global.exception;

/**
 * FastAPI(AI 서비스) 호출이 실패했을 때 던진다. 오케스트레이터가 이걸 잡아서
 * PipelineFailure로 기록하고 해당 단계만 건너뛴다 — 한 단계 실패가 전체 파이프라인을
 * 막지 않게 하기 위함(기존 체크함수 "한 체크가 터져도 나머지는 계속 돈다" 원칙과 동일).
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
