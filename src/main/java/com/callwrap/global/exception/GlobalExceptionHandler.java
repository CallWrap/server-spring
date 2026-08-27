package com.callwrap.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CallWrapException.class)
    public ResponseEntity<ErrorResponse> handleCallWrapException(CallWrapException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ErrorResponse.of(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> details = e.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                (a, b) -> a));
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
            .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, details));
    }

    /**
     * 쿼리 파라미터 타입 변환 실패 (예: ?from=2026-13-99 → LocalDate 파싱 불가).
     *
     * 이걸 여기서 안 잡으면 DefaultHandlerExceptionResolver가 400을 세운 뒤 컨테이너가 /error로
     * 포워드하는데, /error는 SecurityConfig의 anyRequest().authenticated()에 걸려서 최종 응답이
     * **403**으로 뒤바뀐다. 잘못된 날짜를 보냈는데 권한 오류가 오면 원인을 찾을 수가 없다.
     * 여기서 처리하면 포워드 자체가 일어나지 않고 다른 API와 같은 ErrorResponse 형식으로 나간다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        Map<String, String> details = Map.of(
            e.getName(), "형식이 올바르지 않습니다: " + e.getValue());
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
            .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, details));
    }
}
