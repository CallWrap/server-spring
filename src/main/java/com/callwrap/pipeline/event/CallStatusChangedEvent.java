package com.callwrap.pipeline.event;

import com.callwrap.review.domain.CallStatus;

import java.util.UUID;

/**
 * 콜 상태가 바뀔 때마다 발행된다 (JobDispatchService/ResultProcessingService/ReviewConfirmService).
 * CallStatusEventListener가 트랜잭션 커밋 후에만 SSE로 내보낸다 — 롤백된 변경을
 * 화면에 미리 알리지 않기 위해서다.
 */
public record CallStatusChangedEvent(UUID callId, CallStatus status) {
}
