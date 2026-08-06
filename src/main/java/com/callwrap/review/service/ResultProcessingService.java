package com.callwrap.pipeline.service;

import com.callwrap.analysis.domain.AnalysisResult;
import com.callwrap.analysis.domain.PipelineFailure;
import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.pipeline.messaging.AnalysisPayload;
import com.callwrap.pipeline.messaging.QualityPayload;
import com.callwrap.pipeline.messaging.ResultMessage;
import com.callwrap.qualitycheck.domain.QualityCheckResult;
import com.callwrap.qualitycheck.repository.QualityCheckResultRepository;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

public class ResultProcessingService {
}
package com.callwrap.pipeline.service;

import com.callwrap.analysis.domain.AnalysisResult;
import com.callwrap.analysis.domain.PipelineFailure;
import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.pipeline.messaging.AnalysisPayload;
import com.callwrap.pipeline.messaging.QualityPayload;
import com.callwrap.pipeline.messaging.ResultMessage;
import com.callwrap.qualitycheck.domain.QualityCheckResult;
import com.callwrap.qualitycheck.repository.QualityCheckResultRepository;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultProcessingService {

    private final CallRepository callRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final QualityCheckResultRepository qualityCheckResultRepository;

    @Transactional
    public void process(ResultMessage message) {
        Call call = callRepository.findById(message.callId())
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 콜: " + message.callId()));

        switch (message.status()) {
            case DONE -> handleDone(call, message);
            case NEEDS_HUMAN_REVIEW -> handleNeedsReview(call, message);
            case FAILED -> handleFailed(call, message);
        }
    }

    private void handleDone(Call call, ResultMessage message) {
        AnalysisPayload analysis = message.analysis();
        if (analysis == null) {
            throw new IllegalStateException("status=DONE인데 analysis가 비어있음: callId=" + call.getId());
        }

        AnalysisResult result = AnalysisResult.builder()
                .callId(call.getId())
                .summary(analysis.summary())
                .consultationType(analysis.consultationType())
                .consultationCategory(analysis.consultationCategory())
                .consultationSubcategory(analysis.consultationSubcategory())
                .grade(analysis.grade())
                .needsFollowup(analysis.needsFollowup())
                .needsFaq(analysis.needsFaq())
                .failures(toPipelineFailures(message.failures()))
                .build();
        analysisResultRepository.save(result);
        call.transitionTo(CallStatus.DONE);
    }

    private void handleNeedsReview(Call call, ResultMessage message) {
        QualityPayload quality = message.quality();
        if (quality != null && quality.failedChecks() != null) {
            quality.failedChecks().forEach(checkName ->
                    qualityCheckResultRepository.save(QualityCheckResult.builder()
                            .callId(call.getId())
                            .checkName(checkName)
                            .passed(false)
                            .retryIteration(0)
                            .build()));
        }
        call.transitionTo(CallStatus.NEEDS_HUMAN_REVIEW);
    }

    private void handleFailed(Call call, ResultMessage message) {
        log.warn("[pipeline] 콜 처리 실패 callId={} error={}", call.getId(), message.error());
        call.transitionTo(CallStatus.FAILED);
    }

    private List<PipelineFailure> toPipelineFailures(List<String> failureModuleNames) {
        if (failureModuleNames == null) {
            return List.of();
        }
        // 계약서 failures는 모듈명 문자열만 준다 (reason 없음) — 상세 사유는 아직 계약에 없다.
        return failureModuleNames.stream()
                .map(moduleName -> new PipelineFailure(moduleName, null))
                .toList();
    }
}