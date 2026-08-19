package com.callwrap.pipeline.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
 * 트랜잭션이 실제로 커밋된 뒤에만 SSE로 내보낸다 — ResultProcessingService.process()나
 * ReviewConfirmService.confirm()이 커밋 직전에 실패해서 롤백되면, 화면에는 애초에
 * 아무 일도 없었던 것처럼 보여야 한다.
 */
@Component
@RequiredArgsConstructor
public class CallStatusEventListener {

    private final ReviewEventBroadcaster broadcaster;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCallStatusChanged(CallStatusChangedEvent event) {
        broadcaster.broadcast(event);
    }
}
