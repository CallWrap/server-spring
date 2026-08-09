package com.callwrap.review.service;

import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.callwrap.pipeline.service.JobDispatchService;
import com.callwrap.review.domain.Call;
import com.callwrap.review.dto.ReviewCreateRequest;
import com.callwrap.review.dto.ReviewCreateResponse;
import com.callwrap.review.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewCreateService {

    private final CallRepository callRepository;
    private final JobDispatchService jobDispatchService;

    @Transactional
    public ReviewCreateResponse create(ReviewCreateRequest request) {
        if (callRepository.existsBySourceRequestId(request.sourceRequestId())) {
            throw new CallWrapException(ErrorCode.DUPLICATE_SOURCE_REQUEST);
        }

        Call call = Call.builder()
            .counselorId(UUID.fromString(request.counselorId()))
            .audioPath(request.audioFileUrl())
            .sourceRequestId(request.sourceRequestId())
            .callStartedAt(request.callStartedAt())
            .callDurationSec(request.callDurationSec())
            .build();
        callRepository.save(call);

        // STT~분석이 전부 FastAPI 쪽에서 비동기로 처리되므로, 응답은 즉시 202로 끝내고
        // 실제 처리는 결과 메시지가 돌아올 때까지 기다린다.
        jobDispatchService.dispatch(call, Map.of("agent_id", call.getCounselorId().toString()));

        return new ReviewCreateResponse(call.getId(), call.getStatus().name(), call.getCreatedAt());
    }
}
