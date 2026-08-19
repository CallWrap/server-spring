package com.callwrap.review.service;

import com.callwrap.analysis.domain.AnalysisResult;
import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.global.common.Judgment;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.dto.ReviewDetailResponse;
import com.callwrap.review.dto.ReviewListResponse;
import com.callwrap.review.dto.ReviewSummaryResponse;
import com.callwrap.review.repository.CallRepository;
import com.callwrap.transcript.domain.Utterance;
import com.callwrap.transcript.repository.TranscriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/*
 * 목록 화면은 전사·분석 JSONB 전체를 안 실어보낸다 — AnalysisResult는 등급/후속필요/신뢰도
 * 집계를 뽑는 데만 쓰고(콜 id 목록으로 한 번에 배치 조회), 나머지 필드는 상세 조회에서만 편다.
 */

@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.9;

    private final CallRepository callRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final TranscriptRepository transcriptRepository;

    @Transactional(readOnly = true)
    public ReviewListResponse list(CallStatus status, int page, int size) {
        Page<Call> result = callRepository.searchReviews(status, PageRequest.of(page, size));
        List<UUID> callIds = result.getContent().stream().map(Call::getId).toList();
        Map<UUID, AnalysisResult> analysisByCallId = analysisResultRepository.findByCallIdIn(callIds).stream()
            .collect(Collectors.toMap(AnalysisResult::getCallId, a -> a));

        List<ReviewSummaryResponse> items = result.getContent().stream()
            .map(c -> toSummary(c, analysisByCallId.get(c.getId())))
            .collect(Collectors.toList());
        return new ReviewListResponse(items, page, size, result.getTotalElements());
    }

    private ReviewSummaryResponse toSummary(Call call, AnalysisResult analysis) {
        List<Judgment<Object>> reviewableFields = analysis == null ? List.of() : List.of(
            analysis.getConsultationType(), analysis.getConsultationCategory(),
            analysis.getGrade(), analysis.getSummary(), analysis.getNeedsFollowup());
        int highCount = (int) reviewableFields.stream()
            .filter(j -> j != null && j.confidence() != null && j.confidence() >= HIGH_CONFIDENCE_THRESHOLD)
            .count();
        return new ReviewSummaryResponse(
            call.getId(),
            call.getStatus().name(),
            call.getRetryCount(),
            analysis != null ? analysis.getGrade() : null,
            analysis != null ? analysis.getNeedsFollowup() : null,
            highCount,
            reviewableFields.size(),
            call.getCallStartedAt(),
            call.getCreatedAt(),
            call.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public ReviewDetailResponse getDetail(UUID reviewId) {
        Call call = callRepository.findById(reviewId)
            .orElseThrow(() -> new CallWrapException(ErrorCode.REVIEW_NOT_FOUND));

        AnalysisResult analysis = analysisResultRepository.findByCallId(reviewId).orElse(null);

        // 재시도가 여러 번 있었어도 화면엔 가장 최근 시도의 전사만 보여주면 된다.
        List<Utterance> transcript = transcriptRepository.findFirstByCallIdOrderByRetryIterationDesc(reviewId)
            .map(com.callwrap.transcript.domain.Transcript::getUtterances)
            .orElse(List.of());

        ReviewDetailResponse.ClassificationView classification = new ReviewDetailResponse.ClassificationView(
            analysis != null ? analysis.getConsultationType() : null,
            analysis != null ? analysis.getConsultationCategory() : null,
            analysis != null ? analysis.getConsultationSubcategory() : null,
            analysis != null ? analysis.getGrade() : null
        );

        return new ReviewDetailResponse(
            call.getId(),
            call.getStatus().name(),
            call.getCallStartedAt(),
            call.getCallDurationSec(),
            call.getCreatedAt(),
            classification,
            analysis != null ? analysis.getSummary() : null,
            analysis != null ? analysis.getNeedsFollowup() : null,
            analysis != null ? analysis.getNeedsFaq() : null,
            transcript,
            analysis != null ? toFailureMessages(analysis) : List.of(),
            analysis != null ? analysis.getConfirmedAt() : null,
            analysis != null ? analysis.getConfirmedByCounselorId() : null
        );
    }

    private List<String> toFailureMessages(AnalysisResult analysis) {
        if (analysis.getFailures() == null) {
            return List.of();
        }
        // 계약서 failures는 모듈명 문자열만 준다 (reason 없음, 항상 null) — "summarize: null"처럼
        // 보이지 않도록 모듈명만 내려준다.
        return analysis.getFailures().stream()
            .map(com.callwrap.analysis.domain.PipelineFailure::stage)
            .toList();
    }
}
