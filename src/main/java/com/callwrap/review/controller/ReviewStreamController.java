package com.callwrap.review.controller;

import com.callwrap.pipeline.event.ReviewEventBroadcaster;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/*
 * 목록 폴링 대신 쓰는 실시간 갱신 채널. 일반 REST 핸들러(ReviewController)와 달리
 * 요청 하나가 오래 열려 있는 스트리밍 핸들러라 클래스를 분리했다.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewStreamController {

    private final ReviewEventBroadcaster broadcaster;

    @Operation(
        summary = "검토 목록 실시간 갱신 스트림 (SSE)",
        description = "콜 상태가 바뀔 때마다 {callId, status} 이벤트(call-status-changed)를 보낸다. "
            + "목록을 주기적으로 다시 조회하는 대신 이 스트림을 구독하고, 이벤트가 오면 그 콜만 "
            + "다시 조회하거나 목록을 갱신하면 된다. 연결이 끊기면 브라우저 EventSource가 알아서 재연결한다."
    )
    @GetMapping("/stream")
    public SseEmitter stream() {
        return broadcaster.subscribe();
    }
}
