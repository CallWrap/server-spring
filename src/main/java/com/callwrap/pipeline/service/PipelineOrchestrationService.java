package com.callwrap.pipeline.service;

import com.callwrap.analysis.domain.AnalysisResult;
import com.callwrap.analysis.domain.PipelineFailure;
import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.global.common.Judgment;
import com.callwrap.pipeline.client.AiServiceClient;
import com.callwrap.pipeline.client.dto.CategoryClassificationResponse;
import com.callwrap.pipeline.client.dto.FollowupResult;
import com.callwrap.pipeline.client.dto.QualityGateCheckResult;
import com.callwrap.pipeline.client.dto.QualityGateResponse;
import com.callwrap.pipeline.messaging.VoiceProcessingProducer;
import com.callwrap.qualitycheck.domain.QualityAction;
import com.callwrap.qualitycheck.domain.QualityCheckResult;
import com.callwrap.qualitycheck.repository.QualityCheckResultRepository;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.repository.CallRepository;
import com.callwrap.transcript.domain.Transcript;
import com.callwrap.transcript.domain.Utterance;
import com.callwrap.transcript.repository.TranscriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 콜 하나에 대해 STT -> 화자분리 -> 품질게이트 -> 분류3종 -> 요약/후속처리 -> 결과저장까지
 * 순서대로 조율한다. "언제 무엇을 부를지"만 아는 조정자이고, 실제 호출은 AiServiceClient,
 * 저장은 각 도메인 리포지토리가 담당한다.
 * <p>
 * 품질게이트의 action(pass/retry/human_review) 종합 로직 자체는 FastAPI의 /quality-gate
 * 안에서 기존 orchestrator.py를 그대로 재사용해 계산해서 내려준다 — 여기서는 그 결과를
 * 보고 다음 단계로 갈지, 재처리할지, 사람에게 넘길지만 분기한다 (기존 아키텍처 결정 #5 계승).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineOrchestrationService {

    private final CallRepository callRepository;
    private final TranscriptRepository transcriptRepository;
    private final QualityCheckResultRepository qualityCheckResultRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final AiServiceClient aiServiceClient;
    private final VoiceProcessingProducer voiceProcessingProducer;

    @Value("${callwrap.pipeline.max-retry:2}")
    private int maxRetry;

    @Transactional
    public void process(UUID callId) {
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 콜: " + callId));

        int retryIteration = call.getRetryCount();

        call.transitionTo(CallStatus.TRANSCRIBING);
        var sttResponse = aiServiceClient.transcribe(call.getAudioPath(), call.getId().toString());
        var diarizeResponse = aiServiceClient.diarize(call.getAudioPath(), sttResponse.segments());
        List<Utterance> utterances = diarizeResponse.utterances();

        transcriptRepository.save(Transcript.builder()
                .callId(call.getId())
                .utterances(utterances)
                .retryIteration(retryIteration)
                .build());

        call.transitionTo(CallStatus.QUALITY_CHECK);
        QualityGateResponse gateResponse = aiServiceClient.runQualityGate(utterances);
        persistQualityCheckResults(call.getId(), retryIteration, gateResponse.results());

        QualityAction action = QualityAction.fromValue(gateResponse.action());
        switch (action) {
            case RETRY -> handleRetry(call);
            case HUMAN_REVIEW -> call.transitionTo(CallStatus.NEEDS_HUMAN_REVIEW);
            case PASS -> {
                call.transitionTo(CallStatus.PASSED);
                runAnalysis(call, utterances);
            }
        }
    }

    private void handleRetry(Call call) {
        if (call.getRetryCount() >= maxRetry) {
            // 재시도해도 안 되는 문제라는 뜻 — 사람 검토로 승격 (orchestrator.py 액션 종합 규칙 계승)
            call.transitionTo(CallStatus.NEEDS_HUMAN_REVIEW);
            return;
        }
        call.incrementRetry();
        call.transitionTo(CallStatus.RETRYING);
        voiceProcessingProducer.publish(call.getId());
    }

    private void persistQualityCheckResults(UUID callId, int retryIteration, List<QualityGateCheckResult> results) {
        List<QualityCheckResult> entities = results.stream()
                .map(r -> QualityCheckResult.builder()
                        .callId(callId)
                        .checkName(r.checkName())
                        .passed(r.passed())
                        .score(r.score())
                        .threshold(r.threshold())
                        .higherIsBetter(r.higherIsBetter())
                        .action(r.action())
                        .reason(r.reason())
                        .details(r.details())
                        .retryIteration(retryIteration)
                        .build())
                .toList();
        qualityCheckResultRepository.saveAll(entities);
    }

    private void runAnalysis(Call call, List<Utterance> utterances) {
        call.transitionTo(CallStatus.ANALYZING);

        List<PipelineFailure> failures = new ArrayList<>();

        Judgment<Object> type = safeCall(() -> aiServiceClient.classifyType(utterances), "classify_type", failures);
        CategoryClassificationResponse categoryResponse = safeCall(
                () -> aiServiceClient.classifyCategory(utterances), "classify_category", failures);
        Judgment<Object> grade = safeCall(() -> aiServiceClient.classifyGrade(utterances), "classify_grade", failures);
        Judgment<Object> summary = safeCall(() -> aiServiceClient.summarize(utterances), "summarize", failures);

        Map<String, Object> classificationForFollowup = Map.of(
                "type", type != null ? type.value() : "");
        FollowupResult followup = safeCall(
                () -> aiServiceClient.judgeFollowup(utterances, classificationForFollowup), "followup", failures);

        AnalysisResult analysisResult = AnalysisResult.builder()
                .callId(call.getId())
                .summary(summary)
                .consultationType(type)
                .consultationCategory(categoryResponse != null ? categoryResponse.category() : null)
                .consultationSubcategory(categoryResponse != null ? categoryResponse.subcategory() : null)
                .grade(grade)
                .needsFollowup(followup != null
                        ? Judgment.<Object>of(followup.value(), followup.confidence(), null) : null)
                // needs_faq 판단 담당·엔드포인트는 아직 미정 (decisions.md 미해결 항목) - 연결되면 여기에 채운다.
                .needsFaq(null)
                .failures(failures)
                .build();
        analysisResultRepository.save(analysisResult);

        call.transitionTo(CallStatus.DONE);
    }

    private <T> T safeCall(Supplier<T> call, String stage, List<PipelineFailure> failures) {
        try {
            return call.get();
        } catch (Exception e) {
            log.warn("[pipeline] {} 단계 실패: {}", stage, e.getMessage());
            failures.add(new PipelineFailure(stage, e.getMessage()));
            return null;
        }
    }
}
