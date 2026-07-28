package com.callwrap.review.service;

import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.callwrap.pipeline.messaging.VoiceProcessingProducer;
import com.callwrap.review.domain.Call;
import com.callwrap.review.dto.ReviewCreateRequest;
import com.callwrap.review.dto.ReviewCreateResponse;
import com.callwrap.review.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/*
 * 검토 "생성" 파트. (조회는 ReviewQueryService, 확정은 ReviewConfirmService).
 * source_request_id UNIQUE 제약으로 사보원 측의 중복 전송(재시도)을 걸러낸다
 */
@Service
@RequiredArgsConstructor
public class ReviewCreateService {

    private final CallRepository callRepository;
    private final VoiceProcessingProducer voiceProcessingProducer;

    @Transactional
    public ReviewCreateResponse create(ReviewCreateRequest request) {
        if (callRepository.existsBySourceRequestId(request.sourceRequestId())) {
            throw new CallWrapException(ErrorCode.DUPLICATE_SOURCE_REQUEST);
        }

        Call call = Call.builder()
                .counselorId(UUID.fromString(request.counselorId()))
                .audioPath(request.audioFileUrl())
                .sourceRequestId(request.sourceRequestId())
                .build();
        callRepository.save(call);

        // STT가 느리므로 응답은 즉시 202로 끝내고, 실제 처리는 큐 뒤에서 (기존 FastAPI 설계의
        // "202 Accepted 패턴"을 그대로 계승 — RabbitMQ로 매체만 바뀜)
        voiceProcessingProducer.publish(call.getId());

        return new ReviewCreateResponse(call.getId(), call.getStatus().name(), call.getCreatedAt());
    }
}
