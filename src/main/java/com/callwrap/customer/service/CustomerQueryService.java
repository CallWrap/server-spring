package com.callwrap.customer.service;

import com.callwrap.customer.dto.CustomerCallListResponse;
import com.callwrap.customer.dto.CustomerCallResponse;
import com.callwrap.customer.dto.CustomerDetailResponse;
import com.callwrap.customer.dto.CustomerListResponse;
import com.callwrap.customer.dto.CustomerSummaryResponse;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.callwrap.review.domain.Complainant;
import com.callwrap.review.repository.CallRepository;
import com.callwrap.review.repository.ComplainantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerQueryService {

    private final ComplainantRepository complainantRepository;
    private final CallRepository callRepository;

    @Transactional(readOnly = true)
    public CustomerListResponse list(String query, int page, int size) {
        String normalizedQuery = query != null ? query.trim() : "";
        var customers = complainantRepository.search(normalizedQuery, PageRequest.of(page, size));
        List<UUID> ids = customers.getContent().stream().map(Complainant::getId).toList();
        Map<UUID, CallRepository.CustomerSummaryProjection> statsById = ids.isEmpty()
            ? Map.of()
            : callRepository.summarizeCustomers(ids).stream().collect(Collectors.toMap(
                CallRepository.CustomerSummaryProjection::getCustomerId, row -> row));

        List<CustomerSummaryResponse> items = customers.getContent().stream().map(customer -> {
            var stats = statsById.get(customer.getId());
            return new CustomerSummaryResponse(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getOrg(),
                stats != null ? stats.getTotalCalls() : 0,
                stats != null ? stats.getMaliciousCalls() : 0,
                stats != null ? stats.getLastCalledAt() : null);
        }).toList();
        return new CustomerListResponse(items, page, size, customers.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CustomerDetailResponse detail(UUID customerId) {
        Complainant customer = getCustomer(customerId);
        var stats = callRepository.summarizeCustomer(customerId).orElse(null);

        Map<String, CustomerDetailResponse.MonthlyCount> monthlyByMonth = callRepository
            .countCustomerCallsByMonth(customerId).stream()
            .collect(Collectors.toMap(
                CallRepository.CustomerMonthlyCountProjection::getMonth,
                row -> new CustomerDetailResponse.MonthlyCount(
                    row.getMonth(), row.getTotalCalls(), row.getMaliciousCalls(), row.getPraiseCalls())));
        List<CustomerDetailResponse.MonthlyCount> monthlyCounts = recentSixMonths().stream()
            .map(month -> monthlyByMonth.getOrDefault(
                month, new CustomerDetailResponse.MonthlyCount(month, 0, 0, 0)))
            .toList();

        List<CustomerDetailResponse.LabelCount> topSubcategories = callRepository
            .findCustomerTopSubcategories(customerId, PageRequest.of(0, 5)).stream()
            .map(row -> new CustomerDetailResponse.LabelCount(row.getLabel(), row.getCnt()))
            .toList();

        return new CustomerDetailResponse(
            customer.getId(), customer.getName(), customer.getPhone(), customer.getOrg(),
            stats != null ? stats.getFirstCalledAt() : null,
            new CustomerDetailResponse.GradeCounts(
                stats != null ? stats.getMaliciousCalls() : 0,
                stats != null ? stats.getNormalCalls() : 0,
                stats != null ? stats.getPraiseCalls() : 0),
            monthlyCounts,
            topSubcategories);
    }

    @Transactional(readOnly = true)
    public CustomerCallListResponse calls(UUID customerId, int page, int size) {
        if (!complainantRepository.existsById(customerId)) {
            throw new CallWrapException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        var calls = callRepository.findCustomerCalls(customerId, PageRequest.of(page, size));
        List<CustomerCallResponse> items = calls.getContent().stream()
            .map(row -> new CustomerCallResponse(
                row.getReviewId(), row.getCallNo(), row.getCalledAt(), row.getCounselorName(),
                row.getGrade(), row.getSummary(), row.getCallDurationSec(), row.getReviewSec()))
            .toList();
        return new CustomerCallListResponse(items, page, size, calls.getTotalElements());
    }

    private Complainant getCustomer(UUID customerId) {
        return complainantRepository.findById(customerId)
            .orElseThrow(() -> new CallWrapException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private List<String> recentSixMonths() {
        YearMonth current = YearMonth.now();
        List<String> months = new ArrayList<>(6);
        for (int offset = 5; offset >= 0; offset--) {
            months.add(current.minusMonths(offset).toString());
        }
        return List.copyOf(months);
    }
}
