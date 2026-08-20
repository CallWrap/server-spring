package com.callwrap.pipeline.service;

import com.callwrap.pipeline.event.CallStatusChangedEvent;
import com.callwrap.pipeline.metrics.PipelineMetrics;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/*
 * PROCESSING인 채로 영영 돌아오지 않는 콜을 FAILED로 내린다.
 *
 * 배경: FastAPI 워커 프로세스가 통째로 죽으면(OOM·세그폴트 등) Celery가 WorkerLostError를 만드는데,
 * 이건 태스크 안에서 난 예외가 아니라 부모 풀이 만드는 것이라 PipelineTask.on_failure를 안 탄다.
 * 결과 메시지가 아예 발행되지 않고, Spring 입장에선 Job을 보낸 뒤 아무 소식이 없다.
 * 실제로 macOS에서 MPS가 워커를 SIGABRT로 죽였을 때 콜 하나가 PROCESSING에 영구히 갇혔다 —
 * 상담사 화면엔 "분석 중" 스피너가 영원히 도는 상태로 남는다.
 *
 * 워커 쪽에서 이걸 완전히 막을 방법은 없다(죽는 프로세스는 유언을 남기지 못한다).
 * 그래서 상태 소유자인 Spring이 시간으로 판정한다.
 *
 * 재시도하지 않고 바로 FAILED로 내리는 이유: 오디오를 다시 태우면 워커를 죽인 원인이 그대로 재현돼
 * 크래시 루프가 된다. 사람이 원인을 보고 다시 접수하는 편이 안전하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StuckCallSweeper {

    private final CallRepository callRepository;
    private final PipelineMetrics pipelineMetrics;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 이 시간을 넘겨 PROCESSING이면 워커가 죽은 것으로 본다.
     * 파이프라인 전체(전사+게이트 재시도+분석)가 정상적으로 걸리는 최대 시간보다 넉넉해야 한다 —
     * 짧게 잡으면 그냥 오래 걸리는 콜을 죽인다. Whisper large-v3로 긴 통화를 돌리면 몇 분이 걸린다.
     */
    @Value("${callwrap.pipeline.stuck-call.timeout-minutes}")
    private long timeoutMinutes;

    @Scheduled(fixedDelayString = "${callwrap.pipeline.stuck-call.sweep-interval-ms}")
    @Transactional
    public void sweep() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Call> stuck = callRepository.findByStatusAndUpdatedAtBefore(CallStatus.PROCESSING, threshold);
        if (stuck.isEmpty()) {
            return;
        }

        log.warn("[pipeline] {}분 넘게 PROCESSING에 갇힌 콜 {}건을 FAILED로 내린다", timeoutMinutes, stuck.size());
        for (Call call : stuck) {
            log.warn("[pipeline] 콜 {} 처리 결과 미수신 (마지막 갱신 {}) → FAILED", call.getId(), call.getUpdatedAt());
            call.transitionTo(CallStatus.FAILED);
            // 상태 소유자가 바꿨으니 화면도 같이 갱신한다. 커밋 후에만 나간다(CallStatusEventListener).
            eventPublisher.publishEvent(new CallStatusChangedEvent(call.getId(), CallStatus.FAILED));
            pipelineMetrics.recordStuckCallSwept();
        }
    }
}
