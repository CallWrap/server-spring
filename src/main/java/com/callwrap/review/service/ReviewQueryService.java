package com.callwrap.review.service;

import com.callwrap.analysis.domain.AnalysisResult;
import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.dto.ReviewDetailResponse;
import com.callwrap.review.dto.ReviewListResponse;
import com.callwrap.review.dto.ReviewSummaryResponse;
import com.callwrap.review.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 조회만 책임진다. 전부 @Transactional(readOnly = true) — 대용량 목록 조회 경로에서
 * 불필요한 스냅샷/dirty-checking 비용을 없앤다. 목록 화면은 전사·분석 JSONB 전체를 절대
 * 가져오지 않는다(ReviewSummaryResponse에는 상태값만 있음) — 상세 조회에서만 필요한 걸 편다.
 */
@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    private final CallRepository callRepository;
    private final AnalysisResultRepository analysisResultRepository;

    @Transactional(readOnly = true)
    public ReviewListResponse list(CallStatus status, int page, int size) {
        Page<Call> result = callRepository.searchReviews(status, PageRequest.of(page, size));
        List<ReviewSummaryResponse> items = result.getContent().stream()
                .map(c -> new ReviewSummaryResponse(
                        c.getId(), c.getStatus().name(), c.getRetryCount(), c.getCreatedAt(), c.getUpdatedAt()))
                .collect(Collectors.toList());
        return new ReviewListResponse(items, page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ReviewDetailResponse getDetail(UUID reviewId) {
        Call call = callRepository.findById(reviewId)
                .orElseThrow(() -> new CallWrapException(ErrorCode.REVIEW_NOT_FOUND));

        AnalysisResult analysis = analysisResultRepository.findByCallId(reviewId).orElse(null);

        ReviewDetailResponse.ClassificationView classification = new ReviewDetailResponse.ClassificationView(
                analysis != null ? analysis.getConsultationType() : null,
                analysis != null ? analysis.getConsultationCategory() : null,
                analysis != null ? analysis.getConsultationSubcategory() : null,
                analysis != null ? analysis.getGrade() : null
        );

        return new ReviewDetailResponse(
                call.getId(),
                call.getStatus().name(),
                classification,
                analysis != null ? analysis.getSummary() : null,
                analysis != null ? analysis.getNeedsFollowup() : null,
                analysis != null ? analysis.getNeedsFaq() : null,
                analysis != null ? toFailureMessages(analysis) : List.of(),
                analysis != null ? analysis.getConfirmedAt() : null,
                analysis != null ? analysis.getConfirmedByCounselorId() : null
        );
    }

    private List<String> toFailureMessages(AnalysisResult analysis) {
        if (analysis.getFailures() == null) {
            return List.of();
        }
        return analysis.getFailures().stream()
                .map(f -> f.stage() + ": " + f.reason())
                .toList();
    }
}
