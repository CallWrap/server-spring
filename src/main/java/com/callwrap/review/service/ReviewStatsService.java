package com.callwrap.review.service;

import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.review.dto.ReviewStatsResponse;
import com.callwrap.review.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/*
 * 상태·등급·유형 건수와 평균 검토시간을 DB GROUP BY/AVG로 직접 계산한다. 예전엔 AdminPage가
 * /api/reviews?size=100 응답(calls)을 프론트에서 필터링/카운트해서 썼는데, 그러면
 * (1) 100건을 넘는 순간 뒤에 있는 콜은 통계에서 조용히 빠지고
 * (2) 세는 게 목적인데 분석 결과 JSONB 전체를 다 실어보내는 낭비가 있었다.
 */
@Service
@RequiredArgsConstructor
public class ReviewStatsService {

    private final CallRepository callRepository;
    private final AnalysisResultRepository analysisResultRepository;

    @Transactional(readOnly = true)
    public ReviewStatsResponse getStats() {
        Map<String, Long> statusCounts = callRepository.countByStatus().stream()
            .collect(Collectors.toMap(row -> row.getStatus().name(), CallRepository.StatusCountProjection::getCnt));
        long totalCalls = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        Map<String, Long> gradeCounts = analysisResultRepository.countByGrade().stream()
            .collect(Collectors.toMap(
                AnalysisResultRepository.LabelCountProjection::getLabel,
                AnalysisResultRepository.LabelCountProjection::getCnt));

        Map<String, Long> typeCounts = analysisResultRepository.countByConsultationType().stream()
            .collect(Collectors.toMap(
                AnalysisResultRepository.LabelCountProjection::getLabel,
                AnalysisResultRepository.LabelCountProjection::getCnt));

        Double avgReviewSec = analysisResultRepository.averageReviewSec();

        return new ReviewStatsResponse(totalCalls, statusCounts, gradeCounts, typeCounts, avgReviewSec);
    }
}
