package com.callwrap.review.service;

import com.callwrap.analysis.domain.AnalysisResult;
import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.dto.ReviewConfirmRequest;
import com.callwrap.review.dto.ReviewConfirmResponse;
import com.callwrap.review.repository.CallRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public ReviewConfirmResponse confirm(UUID reviewId, UUID counselorId, ReviewConfirmRequest request) {
        Call call = callRepository.findById(reviewId)
                .orElseThrow(() -> new CallWrapException(ErrorCode.REVIEW_NOT_FOUND));

        AnalysisResult analysisResult = analysisResultRepository.findByCallId(reviewId)
                .orElseThrow(() -> new CallWrapException(ErrorCode.REVIEW_NOT_FOUND));

        if (analysisResult.isConfirmed()) {
            throw new CallWrapException(ErrorCode.REVIEW_ALREADY_CONFIRMED);
        }

        analysisResult.confirm(counselorId, request.editedFields());
        call.transitionTo(CallStatus.CONFIRMED);

        return new ReviewConfirmResponse(
                call.getId(), call.getStatus().name(), analysisResult.getConfirmedAt(), counselorId);
    }
}
