package com.callwrap.review.service;

import com.callwrap.analysis.domain.AnalysisResult;
import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.callwrap.pipeline.event.CallStatusChangedEvent;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.dto.ReviewConfirmRequest;
import com.callwrap.review.dto.ReviewConfirmResponse;
import com.callwrap.review.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/*
 * confirmed_by_counselor_id/confirmed_at은 AnalysisResult.confirm()을 통해서만
 * 채워진다 — "확정 전엔 반드시 NULL" 불변식을 엔티티 메서드로 강제한다
 */

@Service
@RequiredArgsConstructor
public class ReviewConfirmService {

    private final CallRepository callRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReviewConfirmResponse confirm(UUID reviewId, UUID counselorId, ReviewConfirmRequest request) {
        Call call = callRepository.findById(reviewId)
            .orElseThrow(() -> new CallWrapException(ErrorCode.REVIEW_NOT_FOUND));

        AnalysisResult analysisResult = analysisResultRepository.findByCallId(reviewId)
            .orElseThrow(() -> new CallWrapException(ErrorCode.REVIEW_NOT_FOUND));

        analysisResult.applyConfirmedFields(request.confirmedFields());
        // 재확정 허용(백엔드_요청사항 3번, A안) — "진행 중"으로 한 번 확정한 콜을 "완료"로 다시
        // 확정하는 흐름이 정상 케이스라, 이미 확정된 콜이어도 막지 않고 값만 갱신한다.
        if (analysisResult.isConfirmed()) {
            analysisResult.reconfirm(counselorId, request.editedFields(), request.reviewSec());
        } else {
            analysisResult.confirm(counselorId, request.editedFields(), request.reviewSec());
        }
        call.transitionTo(CallStatus.CONFIRMED);
        // 같은 큐를 보는 다른 상담사 화면에서도 이 콜이 바로 사라지도록(중복 작업 방지)
        eventPublisher.publishEvent(new CallStatusChangedEvent(call.getId(), CallStatus.CONFIRMED));

        return new ReviewConfirmResponse(
            call.getId(), call.getStatus().name(), analysisResult.getConfirmedAt(), counselorId);
    }
}
