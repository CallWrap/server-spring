package com.callwrap.pipeline.service;

import com.callwrap.analysis.domain.AnalysisResult;
import com.callwrap.analysis.domain.UtteranceSentiment;
import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.global.common.Judgment;
import com.callwrap.pipeline.event.CallStatusChangedEvent;
import com.callwrap.pipeline.messaging.AnalysisPayload;
import com.callwrap.pipeline.messaging.QualityPayload;
import com.callwrap.pipeline.messaging.ResultMessage;
import com.callwrap.pipeline.messaging.ResultStatus;
import com.callwrap.qualitycheck.repository.QualityCheckResultRepository;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.repository.CallRepository;
import com.callwrap.transcript.repository.TranscriptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultProcessingServiceTest {

    @Mock
    private CallRepository callRepository;
    @Mock
    private AnalysisResultRepository analysisResultRepository;
    @Mock
    private QualityCheckResultRepository qualityCheckResultRepository;
    @Mock
    private TranscriptRepository transcriptRepository;
    @Mock
    private com.callwrap.pipeline.metrics.PipelineMetrics pipelineMetrics;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void DONE이면_분석결과를_저장하고_콜을_DONE으로_전이한다() {
        Call call = call();
        when(callRepository.findById(call.getId())).thenReturn(Optional.of(call));

        AnalysisPayload analysis = new AnalysisPayload(
            Judgment.of("요약", 0.9, List.of(0)), Judgment.of("사용법문의", 0.9, List.of(0)),
            Judgment.of("결제및비용지급", 0.9, List.of(0)), Judgment.of("결제방법 및 오류", 0.9, List.of(0)),
            Judgment.of("일반", 0.9, List.of()),
            List.of(
                new UtteranceSentiment(1, "민원인", -1.0),
                new UtteranceSentiment(2, "상담사", -0.5),
                new UtteranceSentiment(3, "상담사", -0.1),
                new UtteranceSentiment(4, "상담사", 0.25)),
            null, Judgment.of(true, 0.9, List.of()));
        ResultMessage message = new ResultMessage(
            "1.0", call.getId(), ResultStatus.DONE, analysis, null, null, List.of(), null, Instant.now());

        service().process(message);

        ArgumentCaptor<AnalysisResult> captor = ArgumentCaptor.forClass(AnalysisResult.class);
        verify(analysisResultRepository).save(captor.capture());
        assertThat(captor.getValue().getUtteranceSentiments())
            .hasSize(4);
        assertThat(call.getCounselorNegativeScore()).isCloseTo(0.3, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(call.getStatus()).isEqualTo(CallStatus.DONE);
        verify(eventPublisher).publishEvent(new CallStatusChangedEvent(call.getId(), CallStatus.DONE));
    }

    @Test
    void NEEDS_HUMAN_REVIEW면_실패한_체크를_저장하고_콜을_전이한다() {
        Call call = call();
        when(callRepository.findById(call.getId())).thenReturn(Optional.of(call));

        QualityPayload quality = new QualityPayload(false, List.of("check_speaker_overlap", "check_segment_length"));
        ResultMessage message = new ResultMessage(
            "1.0", call.getId(), ResultStatus.NEEDS_HUMAN_REVIEW, null, null, quality, List.of(), null, Instant.now());

        service().process(message);

        verify(qualityCheckResultRepository, times(2)).save(any());
        assertThat(call.getStatus()).isEqualTo(CallStatus.NEEDS_HUMAN_REVIEW);
    }

    @Test
    void FAILED면_저장없이_콜을_FAILED로_전이한다() {
        Call call = call();
        when(callRepository.findById(call.getId())).thenReturn(Optional.of(call));

        ResultMessage message = new ResultMessage(
            "1.0", call.getId(), ResultStatus.FAILED, null, null, null, List.of(), "AudioNotFound", Instant.now());

        service().process(message);

        verify(analysisResultRepository, org.mockito.Mockito.never()).save(any());
        assertThat(call.getStatus()).isEqualTo(CallStatus.FAILED);
    }

    private ResultProcessingService service() {
        return new ResultProcessingService(
            callRepository, analysisResultRepository, qualityCheckResultRepository, transcriptRepository,
            pipelineMetrics, eventPublisher);
    }

    private Call call() {
        Call call = Call.builder().counselorId(UUID.randomUUID()).audioPath("a.wav").sourceRequestId("SRC-1").build();
        org.springframework.test.util.ReflectionTestUtils.setField(call, "id", UUID.randomUUID());
        org.springframework.test.util.ReflectionTestUtils.setField(call, "updatedAt", java.time.LocalDateTime.now());
        return call;
    }
}
