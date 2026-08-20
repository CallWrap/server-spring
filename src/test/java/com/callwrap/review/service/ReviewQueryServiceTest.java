package com.callwrap.review.service;

import com.callwrap.analysis.domain.AnalysisResult;
import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.global.common.Judgment;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.dto.ReviewListResponse;
import com.callwrap.review.repository.CallRepository;
import com.callwrap.transcript.repository.TranscriptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/*
 * 대기열 목록은 UUID만으로는 어떤 콜인지 알 수 없다 — 요약 제목이 실려 나가는지 확인한다.
 * Summary는 JSONB에서 Map으로 올라오므로(Judgment<Object>) 그 형태 그대로 테스트한다.
 */
@ExtendWith(MockitoExtension.class)
class ReviewQueryServiceTest {

    @Mock
    private CallRepository callRepository;
    @Mock
    private AnalysisResultRepository analysisResultRepository;
    @Mock
    private TranscriptRepository transcriptRepository;
    @InjectMocks
    private ReviewQueryService reviewQueryService;

    @Test
    void 목록에_접수순번이_실린다() {
        Call call = call();
        ReflectionTestUtils.setField(call, "callNo", 7);
        when(callRepository.searchReviews(any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(call)));
        when(analysisResultRepository.findByCallIdIn(List.of(call.getId()))).thenReturn(List.of());

        ReviewListResponse result = reviewQueryService.list(null, 0, 20);

        assertThat(result.items()).singleElement().extracting("callNo").isEqualTo(7);
    }

    @Test
    void 목록에_요약_제목이_실린다() {
        Call call = call();
        when(callRepository.searchReviews(any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(call)));
        when(analysisResultRepository.findByCallIdIn(List.of(call.getId())))
            .thenReturn(List.of(analysisWithTitle(call.getId(), "지난달 과오결제 취소 요청")));

        ReviewListResponse result = reviewQueryService.list(null, 0, 20);

        assertThat(result.items()).singleElement()
            .extracting("title").isEqualTo("지난달 과오결제 취소 요청");
    }

    @Test
    void 분석_전이면_제목은_null이다() {
        Call call = call();
        when(callRepository.searchReviews(any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(call)));
        when(analysisResultRepository.findByCallIdIn(List.of(call.getId()))).thenReturn(List.of());

        ReviewListResponse result = reviewQueryService.list(null, 0, 20);

        assertThat(result.items()).singleElement().extracting("title").isNull();
    }

    @Test
    void 부분_실패로_판정이_비어도_목록이_뜬다() {
        // Ollama가 꺼지면 grade·summary·needsFollowup이 전부 null로 들어온다 (부분 실패 허용).
        // 이때도 대기열은 떠야 한다 — 예전엔 여기서 목록 API 전체가 NPE로 죽었다.
        Call call = call();
        when(callRepository.searchReviews(any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(call)));
        when(analysisResultRepository.findByCallIdIn(List.of(call.getId())))
            .thenReturn(List.of(AnalysisResult.builder().callId(call.getId()).build()));

        ReviewListResponse result = reviewQueryService.list(null, 0, 20);

        assertThat(result.items()).singleElement()
            .satisfies(item -> {
                assertThat(item.title()).isNull();
                assertThat(item.highConfidenceFieldCount()).isZero();
                assertThat(item.totalFieldCount()).isEqualTo(5);
            });
    }

    private AnalysisResult analysisWithTitle(UUID callId, String title) {
        AnalysisResult analysis = AnalysisResult.builder()
            .callId(callId)
            .summary(Judgment.of(Map.of("title", title, "inquiry", "문의", "resolution", "처리"),
                0.88, List.of(1)))
            .build();
        return analysis;
    }

    private Call call() {
        Call call = Call.builder()
            .counselorId(UUID.randomUUID()).audioPath("a.wav").sourceRequestId("SRC-Q1").build();
        ReflectionTestUtils.setField(call, "id", UUID.randomUUID());
        call.transitionTo(CallStatus.DONE);
        return call;
    }
}
