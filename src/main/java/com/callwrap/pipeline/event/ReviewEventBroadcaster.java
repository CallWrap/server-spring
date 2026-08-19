package com.callwrap.pipeline.event;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * 콜 목록 화면(상담사 여러 명이 같은 큐를 본다)을 폴링 없이 갱신하기 위한 SSE 브로드캐스터.
 * 상담사별로 볼 수 있는 콜이 갈리지 않으므로(ReviewQueryService.list()에 counselorId 필터가
 * 없음) 필터링 없이 전체 브로드캐스트한다.
 *
 * 단일 인스턴스 배포(POC) 전제로 emitter를 메모리에만 들고 있다. 여러 인스턴스로 스케일하면
 * Redis pub/sub 등 공유 브로커로 바꿔야 한다 — 지금은 그 정도 규모가 아니라 오버엔지니어링으로 보류.
 *
 * nginx 등 리버스 프록시가 idle 커넥션을 끊는 걸 막기 위해 heartbeat(주석 이벤트)를 주기적으로 보낸다.
 */
@Slf4j
@Component
public class ReviewEventBroadcaster {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L; // 30분 — 넘으면 브라우저가 알아서 재연결

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        log.info("[sse] 검토 목록 스트림 구독 (현재 {}개 연결)", emitters.size());
        return emitter;
    }

    public void broadcast(CallStatusChangedEvent event) {
        send(emitter -> emitter.send(SseEmitter.event()
            .name("call-status-changed")
            .data(new CallStatusPayload(event.callId(), event.status().name()))));
    }

    @Scheduled(fixedDelay = 20_000)
    public void heartbeat() {
        if (emitters.isEmpty()) {
            return;
        }
        send(emitter -> emitter.send(SseEmitter.event().comment("ping")));
    }

    private void send(EmitterAction action) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                action.run(emitter);
            } catch (IOException | IllegalStateException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    @PreDestroy
    public void shutdown() {
        emitters.forEach(SseEmitter::complete);
        emitters.clear();
    }

    @FunctionalInterface
    private interface EmitterAction {
        void run(SseEmitter emitter) throws IOException;
    }

    public record CallStatusPayload(UUID callId, String status) {
    }
}
