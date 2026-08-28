package com.callwrap.counselor.service;

import com.callwrap.counselor.config.CounselorHealthProperties;
import com.callwrap.counselor.dto.CounselorCallListResponse;
import com.callwrap.counselor.dto.CounselorDetailResponse;
import com.callwrap.counselor.dto.CounselorSummaryResponse;
import com.callwrap.counselor.repository.CounselorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounselorAdminServiceTest {

    @Mock
    private CounselorRepository counselorRepository;

    private CounselorAdminService counselorAdminService;

    @BeforeEach
    void setUp() {
        var properties = new CounselorHealthProperties(7, 0.25, 0.45, 2, 4);
        counselorAdminService = new CounselorAdminService(
            counselorRepository, new CounselorStateEvaluator(properties), properties);
    }

    @Test
    void 상담사_목록은_선택일_집계와_최근7일_상태를_반환한다() {
        UUID counselorId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 28);
        when(counselorRepository.findAdminSummaries(
            date, LocalDate.of(2026, 8, 22), date))
            .thenReturn(List.of(summary(counselorId, "김상담", "복지상담팀", 8, 2, 330.4, 71.8, 0.31, 2)));

        List<CounselorSummaryResponse> result = counselorAdminService.list(date);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.name()).isEqualTo("김상담");
            assertThat(item.totalCalls()).isEqualTo(8);
            assertThat(item.avgCallSec()).isEqualTo(330);
            assertThat(item.avgReviewSec()).isEqualTo(72);
            assertThat(item.state()).isEqualTo("주의");
        });
    }

    @Test
    void 상담사_상세는_등급_추이_판정근거를_반환한다() {
        UUID counselorId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 28);
        when(counselorRepository.findAdminDetail(
            counselorId, date, LocalDate.of(2026, 8, 22), date))
            .thenReturn(Optional.of(detail(counselorId, 6, 1980, 1, 4, 1, 0.52, 1)));
        when(counselorRepository.findSentimentTrend(
            counselorId, LocalDate.of(2026, 8, 22), date))
            .thenReturn(List.of(trend(LocalDate.of(2026, 8, 27), 0.2), trend(date, 0.5)));

        CounselorDetailResponse result = counselorAdminService.detail(counselorId, date);

        assertThat(result.gradeCounts().malicious()).isEqualTo(1);
        assertThat(result.gradeCounts().normal()).isEqualTo(4);
        assertThat(result.sentimentTrend()).hasSize(7);
        assertThat(result.sentimentTrend().get(5).negativeScore()).isEqualTo(0.2);
        assertThat(result.state()).isEqualTo("휴식 권장");
        assertThat(result.stateReason()).contains("부정 발화");
    }

    @Test
    void 상담사_담당콜은_상담사_발화의_부정점수를_포함한다() {
        UUID counselorId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 28);
        when(counselorRepository.existsById(counselorId)).thenReturn(true);
        when(counselorRepository.findCounselorCalls(counselorId, date, PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(List.of(call(reviewId, 19, 0.42))));

        CounselorCallListResponse result = counselorAdminService.calls(counselorId, date, 0, 20);

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.reviewId()).isEqualTo(reviewId);
            assertThat(item.negativeScore()).isEqualTo(0.42);
            assertThat(item.customerName()).isEqualTo("임세라");
        });
    }

    private CounselorRepository.CounselorSummaryProjection summary(
            UUID id, String name, String team, long total, long malicious,
            double avgCall, double avgReview, double healthNegative, long healthMalicious) {
        return new CounselorRepository.CounselorSummaryProjection() {
            public UUID getCounselorId() { return id; }
            public String getName() { return name; }
            public String getTeam() { return team; }
            public long getTotalCalls() { return total; }
            public long getMaliciousCalls() { return malicious; }
            public Double getAvgCallSec() { return avgCall; }
            public Double getAvgReviewSec() { return avgReview; }
            public Double getHealthNegativeScore() { return healthNegative; }
            public long getHealthMaliciousCalls() { return healthMalicious; }
        };
    }

    private CounselorRepository.CounselorDetailProjection detail(
            UUID id, long total, long totalSec, long malicious, long normal, long praise,
            double healthNegative, long healthMalicious) {
        return new CounselorRepository.CounselorDetailProjection() {
            public UUID getCounselorId() { return id; }
            public String getName() { return "김상담"; }
            public String getTeam() { return "복지상담팀"; }
            public long getTotalCalls() { return total; }
            public long getTotalCallSec() { return totalSec; }
            public long getMaliciousCalls() { return malicious; }
            public long getNormalCalls() { return normal; }
            public long getPraiseCalls() { return praise; }
            public Double getHealthNegativeScore() { return healthNegative; }
            public long getHealthMaliciousCalls() { return healthMalicious; }
        };
    }

    private CounselorRepository.SentimentTrendProjection trend(LocalDate date, double score) {
        return new CounselorRepository.SentimentTrendProjection() {
            public LocalDate getDate() { return date; }
            public Double getNegativeScore() { return score; }
        };
    }

    private CounselorRepository.CounselorCallProjection call(UUID reviewId, int callNo, double score) {
        return new CounselorRepository.CounselorCallProjection() {
            public UUID getReviewId() { return reviewId; }
            public int getCallNo() { return callNo; }
            public LocalDateTime getCalledAt() { return LocalDateTime.of(2026, 8, 28, 9, 20); }
            public String getCustomerName() { return "임세라"; }
            public String getGrade() { return "일반"; }
            public String getSummary() { return "로그인 문의"; }
            public Integer getCallDurationSec() { return 320; }
            public Integer getReviewSec() { return 64; }
            public Double getNegativeScore() { return score; }
        };
    }
}
