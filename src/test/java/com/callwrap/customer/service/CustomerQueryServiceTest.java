package com.callwrap.customer.service;

import com.callwrap.customer.dto.CustomerCallListResponse;
import com.callwrap.customer.dto.CustomerDetailResponse;
import com.callwrap.customer.dto.CustomerListResponse;
import com.callwrap.review.domain.Complainant;
import com.callwrap.review.repository.CallRepository;
import com.callwrap.review.repository.ComplainantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerQueryServiceTest {

    @Mock
    private ComplainantRepository complainantRepository;
    @Mock
    private CallRepository callRepository;
    @InjectMocks
    private CustomerQueryService customerQueryService;

    @Test
    void 민원인_목록은_누적상담_악성상담_최근상담을_함께_반환한다() {
        Complainant customer = customer();
        LocalDateTime lastCalledAt = LocalDateTime.of(2026, 8, 27, 15, 30);
        when(complainantRepository.search("홍길", PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(List.of(customer)));
        when(callRepository.summarizeCustomers(List.of(customer.getId())))
            .thenReturn(List.of(customerSummary(customer.getId(), 7L, 2L, lastCalledAt)));

        CustomerListResponse result = customerQueryService.list("  홍길  ", 0, 20);

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.name()).isEqualTo("홍길동");
            assertThat(item.totalCalls()).isEqualTo(7);
            assertThat(item.maliciousCalls()).isEqualTo(2);
            assertThat(item.lastCalledAt()).isEqualTo(lastCalledAt);
        });
    }

    @Test
    void 민원인_상세는_등급별_누계와_최근6개월_문의추이와_상위소분류를_반환한다() {
        Complainant customer = customer();
        LocalDateTime firstCalledAt = LocalDateTime.of(2026, 3, 2, 9, 0);
        when(complainantRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(callRepository.summarizeCustomer(customer.getId()))
            .thenReturn(Optional.of(customerDetail(12L, 3L, 7L, 2L, firstCalledAt)));
        when(callRepository.countCustomerCallsByMonth(customer.getId()))
            .thenReturn(List.of(monthCount("2026-07", 4L, 1L, 1L), monthCount("2026-08", 3L, 0L, 1L)));
        when(callRepository.findCustomerTopSubcategories(customer.getId(), PageRequest.of(0, 5)))
            .thenReturn(List.of(labelCount("로그인", 5L), labelCount("급여 확인", 3L)));

        CustomerDetailResponse result = customerQueryService.detail(customer.getId());

        assertThat(result.firstCalledAt()).isEqualTo(firstCalledAt);
        assertThat(result.gradeCounts().malicious()).isEqualTo(3);
        assertThat(result.gradeCounts().normal()).isEqualTo(7);
        assertThat(result.gradeCounts().praise()).isEqualTo(2);
        assertThat(result.monthlyCounts()).hasSize(6);
        assertThat(result.monthlyCounts()).extracting("month")
            .containsExactly("2026-03", "2026-04", "2026-05", "2026-06", "2026-07", "2026-08");
        assertThat(result.monthlyCounts().get(4).total()).isEqualTo(4);
        assertThat(result.topSubcategories()).extracting("label")
            .containsExactly("로그인", "급여 확인");
    }

    @Test
    void 민원인_상담이력은_상담사_등급_요약_통화시간을_반환한다() {
        Complainant customer = customer();
        UUID callId = UUID.randomUUID();
        LocalDateTime calledAt = LocalDateTime.of(2026, 8, 28, 10, 20);
        when(complainantRepository.existsById(customer.getId())).thenReturn(true);
        when(callRepository.findCustomerCalls(customer.getId(), PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(List.of(customerCall(
                callId, 31, calledAt, "김상담", "악성콜", "로그인 반복 오류", 420, 75))));

        CustomerCallListResponse result = customerQueryService.calls(customer.getId(), 0, 20);

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.reviewId()).isEqualTo(callId);
            assertThat(item.callNo()).isEqualTo(31);
            assertThat(item.counselorName()).isEqualTo("김상담");
            assertThat(item.grade()).isEqualTo("악성콜");
            assertThat(item.summary()).isEqualTo("로그인 반복 오류");
            assertThat(item.callDurationSec()).isEqualTo(420);
            assertThat(item.reviewSec()).isEqualTo(75);
        });
    }

    private Complainant customer() {
        Complainant customer = Complainant.builder()
            .name("홍길동").phone("010-1234-5678").org("복지정책과").build();
        ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());
        return customer;
    }

    private CallRepository.CustomerSummaryProjection customerSummary(
            UUID id, long total, long malicious, LocalDateTime lastCalledAt) {
        return new CallRepository.CustomerSummaryProjection() {
            public UUID getCustomerId() { return id; }
            public long getTotalCalls() { return total; }
            public long getMaliciousCalls() { return malicious; }
            public LocalDateTime getLastCalledAt() { return lastCalledAt; }
        };
    }

    private CallRepository.CustomerDetailProjection customerDetail(
            long total, long malicious, long normal, long praise, LocalDateTime firstCalledAt) {
        return new CallRepository.CustomerDetailProjection() {
            public long getTotalCalls() { return total; }
            public long getMaliciousCalls() { return malicious; }
            public long getNormalCalls() { return normal; }
            public long getPraiseCalls() { return praise; }
            public LocalDateTime getFirstCalledAt() { return firstCalledAt; }
        };
    }

    private CallRepository.CustomerMonthlyCountProjection monthCount(
            String month, long total, long malicious, long praise) {
        return new CallRepository.CustomerMonthlyCountProjection() {
            public String getMonth() { return month; }
            public long getTotalCalls() { return total; }
            public long getMaliciousCalls() { return malicious; }
            public long getPraiseCalls() { return praise; }
        };
    }

    private CallRepository.LabelCountProjection labelCount(String label, long count) {
        return new CallRepository.LabelCountProjection() {
            public String getLabel() { return label; }
            public long getCnt() { return count; }
        };
    }

    private CallRepository.CustomerCallProjection customerCall(
            UUID reviewId, int callNo, LocalDateTime calledAt, String counselorName,
            String grade, String summary, int callDurationSec, int reviewSec) {
        return new CallRepository.CustomerCallProjection() {
            public UUID getReviewId() { return reviewId; }
            public int getCallNo() { return callNo; }
            public LocalDateTime getCalledAt() { return calledAt; }
            public String getCounselorName() { return counselorName; }
            public String getGrade() { return grade; }
            public String getSummary() { return summary; }
            public Integer getCallDurationSec() { return callDurationSec; }
            public Integer getReviewSec() { return reviewSec; }
        };
    }
}
