package com.callwrap.review.service;

import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.analysis.repository.AnalysisResultRepository.LabelCountProjection;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.dto.ReviewStatsResponse;
import com.callwrap.review.repository.CallRepository;
import com.callwrap.review.repository.CallRepository.StatusCountProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewStatsServiceTest {

    @Mock
    private CallRepository callRepository;
    @Mock
    private AnalysisResultRepository analysisResultRepository;

    @Test
    void 상태_등급_유형별_건수와_평균_검토시간을_그대로_담아_반환한다() {
        when(callRepository.countByStatus()).thenReturn(List.of(
            statusCount(CallStatus.CONFIRMED, 7L),
            statusCount(CallStatus.NEEDS_HUMAN_REVIEW, 3L)
        ));
        when(analysisResultRepository.countByGrade()).thenReturn(List.of(
            labelCount("일반", 8L),
            labelCount("악성콜", 2L)
        ));
        when(analysisResultRepository.countByConsultationType()).thenReturn(List.of(
            labelCount("미분류", 10L)
        ));
        when(analysisResultRepository.countByConsultationCategory()).thenReturn(List.of(
            labelCount("사용법문의", 6L),
            labelCount("요금문의", 4L)
        ));
        when(analysisResultRepository.averageReviewSec()).thenReturn(142.5);
        when(analysisResultRepository.getConfidenceStats()).thenReturn(confidenceStats(0L, 0L, null));

        ReviewStatsResponse stats = service().getStats();

        assertThat(stats.totalCalls()).isEqualTo(10L);
        assertThat(stats.statusCounts()).containsEntry("CONFIRMED", 7L).containsEntry("NEEDS_HUMAN_REVIEW", 3L);
        assertThat(stats.gradeCounts()).containsEntry("일반", 8L).containsEntry("악성콜", 2L);
        assertThat(stats.typeCounts()).containsEntry("미분류", 10L);
        assertThat(stats.categoryCounts()).containsEntry("사용법문의", 6L).containsEntry("요금문의", 4L);
        assertThat(stats.avgReviewSec()).isEqualTo(142.5);
    }

    @Test
    void 확정된_콜이_없으면_평균_검토시간은_null이다() {
        when(callRepository.countByStatus()).thenReturn(List.of());
        when(analysisResultRepository.countByGrade()).thenReturn(List.of());
        when(analysisResultRepository.countByConsultationType()).thenReturn(List.of());
        when(analysisResultRepository.countByConsultationCategory()).thenReturn(List.of());
        when(analysisResultRepository.averageReviewSec()).thenReturn(null);
        when(analysisResultRepository.getConfidenceStats()).thenReturn(confidenceStats(0L, 0L, null));

        ReviewStatsResponse stats = service().getStats();

        assertThat(stats.totalCalls()).isZero();
        assertThat(stats.avgReviewSec()).isNull();
    }

    private ReviewStatsService service() {
        return new ReviewStatsService(callRepository, analysisResultRepository);
    }

    private StatusCountProjection statusCount(CallStatus status, Long cnt) {
        return new StatusCountProjection() {
            public CallStatus getStatus() { return status; }
            public Long getCnt() { return cnt; }
        };
    }

    private LabelCountProjection labelCount(String label, Long cnt) {
        return new LabelCountProjection() {
            public String getLabel() { return label; }
            public Long getCnt() { return cnt; }
        };
    }

    private AnalysisResultRepository.ConfidenceStatsProjection confidenceStats(
            Long totalJudgedCount, Long highConfidenceCount, Double avgConfidence) {
        return new AnalysisResultRepository.ConfidenceStatsProjection() {
            public Long getTotalJudgedCount() { return totalJudgedCount; }
            public Long getHighConfidenceCount() { return highConfidenceCount; }
            public Double getAvgConfidence() { return avgConfidence; }
        };
    }
}
