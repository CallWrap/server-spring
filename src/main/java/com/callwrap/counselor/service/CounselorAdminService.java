package com.callwrap.counselor.service;

import com.callwrap.counselor.config.CounselorHealthProperties;
import com.callwrap.counselor.dto.CounselorCallListResponse;
import com.callwrap.counselor.dto.CounselorCallResponse;
import com.callwrap.counselor.dto.CounselorDetailResponse;
import com.callwrap.counselor.dto.CounselorSummaryResponse;
import com.callwrap.counselor.repository.CounselorRepository;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CounselorAdminService {

    private final CounselorRepository counselorRepository;
    private final CounselorStateEvaluator stateEvaluator;
    private final CounselorHealthProperties healthProperties;

    @Transactional(readOnly = true)
    public List<CounselorSummaryResponse> list(LocalDate date) {
        LocalDate healthFrom = healthFrom(date);
        return counselorRepository.findAdminSummaries(date, healthFrom, date).stream()
            .map(row -> {
                var state = stateEvaluator.evaluate(
                    valueOrZero(row.getHealthNegativeScore()), row.getHealthMaliciousCalls());
                return new CounselorSummaryResponse(
                    row.getCounselorId(), row.getName(), row.getTeam(), row.getTotalCalls(),
                    row.getMaliciousCalls(), rounded(row.getAvgCallSec()), rounded(row.getAvgReviewSec()),
                    state.state());
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public CounselorDetailResponse detail(UUID counselorId, LocalDate date) {
        LocalDate healthFrom = healthFrom(date);
        var row = counselorRepository.findAdminDetail(counselorId, date, healthFrom, date)
            .orElseThrow(() -> new CallWrapException(ErrorCode.COUNSELOR_NOT_FOUND));
        var state = stateEvaluator.evaluate(
            valueOrZero(row.getHealthNegativeScore()), row.getHealthMaliciousCalls());

        Map<LocalDate, Double> scoreByDate = counselorRepository
            .findSentimentTrend(counselorId, healthFrom, date).stream()
            .collect(Collectors.toMap(
                CounselorRepository.SentimentTrendProjection::getDate,
                trend -> valueOrZero(trend.getNegativeScore())));
        List<CounselorDetailResponse.SentimentPoint> trend = healthFrom.datesUntil(date.plusDays(1))
            .map(day -> new CounselorDetailResponse.SentimentPoint(
                day, scoreByDate.getOrDefault(day, 0.0)))
            .toList();

        return new CounselorDetailResponse(
            row.getCounselorId(), row.getName(), row.getTeam(), row.getTotalCalls(), row.getTotalCallSec(),
            new CounselorDetailResponse.GradeCounts(
                row.getMaliciousCalls(), row.getNormalCalls(), row.getPraiseCalls()),
            trend, state.state(), state.reason());
    }

    @Transactional(readOnly = true)
    public CounselorCallListResponse calls(UUID counselorId, LocalDate date, int page, int size) {
        if (!counselorRepository.existsById(counselorId)) {
            throw new CallWrapException(ErrorCode.COUNSELOR_NOT_FOUND);
        }
        var calls = counselorRepository.findCounselorCalls(
            counselorId, date, PageRequest.of(page, size));
        List<CounselorCallResponse> items = calls.getContent().stream()
            .map(row -> new CounselorCallResponse(
                row.getReviewId(), row.getCallNo(), row.getCalledAt(), row.getCustomerName(), row.getGrade(),
                row.getSummary(), row.getCallDurationSec(), row.getReviewSec(), valueOrZero(row.getNegativeScore())))
            .toList();
        return new CounselorCallListResponse(items, page, size, calls.getTotalElements());
    }

    private LocalDate healthFrom(LocalDate date) {
        return date.minusDays(Math.max(1, healthProperties.lookbackDays()) - 1L);
    }

    private long rounded(Double value) {
        return value != null ? Math.round(value) : 0;
    }

    private double valueOrZero(Double value) {
        return value != null ? value : 0.0;
    }
}
