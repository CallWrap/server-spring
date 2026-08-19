package com.callwrap.pipeline.service;

import com.callwrap.pipeline.event.CallStatusChangedEvent;
import com.callwrap.pipeline.messaging.JobRequestPublisher;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

//트랜잭셔널 아웃박스는 오버엔지니어링으로 판단되어 일단 보류하였습니다.

@Service
@RequiredArgsConstructor
public class JobDispatchService {

    private final JobRequestPublisher jobRequestPublisher;
    private final ApplicationEventPublisher eventPublisher;

    public void dispatch(Call call, Map<String, Object> metadata) {
        jobRequestPublisher.publish(
                call.getId(),
                call.getAudioPath(),
                resolveAudioFormat(call.getAudioPath()),
                metadata
        );
        call.transitionTo(CallStatus.PROCESSING);
        eventPublisher.publishEvent(new CallStatusChangedEvent(call.getId(), CallStatus.PROCESSING));
    }

    private String resolveAudioFormat(String audioUri) {
        int dotIndex = audioUri.lastIndexOf('.');
        return dotIndex >= 0 ? audioUri.substring(dotIndex + 1) : null;
    }
}