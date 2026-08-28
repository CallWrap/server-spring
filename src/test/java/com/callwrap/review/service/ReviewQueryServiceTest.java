package com.callwrap.review.service;

import com.callwrap.analysis.domain.AnalysisResult;
import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.counselor.domain.Counselor;
import com.callwrap.counselor.repository.CounselorRepository;
import com.callwrap.global.common.Judgment;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.dto.ReviewListResponse;
import com.callwrap.review.repository.CallRepository;
import com.callwrap.review.repository.ComplainantRepository;
import com.callwrap.review.repository.FollowupNoteRepository;
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
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private ComplainantRepository complainantRepository;
    @Mock
    private FollowupNoteRepository followupNoteRepository;
    @Mock
    private CounselorRepository counselorRepository;
    @InjectMocks
    private ReviewQueryService reviewQueryService;

    @Test
    void 목록에_접수순번이_실린다() {
        Call call = call();
        ReflectionTestUtils.setField(call, "callNo", 7);
        when(callRepository.searchReviews(any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(call)));
        when(analysisResultRepository.findByCallIdIn(List.of(call.getId()))).thenReturn(List.of());

        ReviewListResponse result = reviewQueryService.list(UUID.randomUUID(), null, null, null, 0, 20);

        assertThat(result.items()).singleElement().extracting("callNo").isEqualTo(7);
    }

    @Test
    void 목록에_요약_제목이_실린다() {
        Call call = call();
        when(callRepository.searchReviews(any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(call)));
        when(analysisResultRepository.findByCallIdIn(List.of(call.getId())))
            .thenReturn(List.of(analysisWithTitle(call.getId(), "지난달 과오결제 취소 요청")));

        ReviewListResponse result = reviewQueryService.list(UUID.randomUUID(), null, null, null, 0, 20);

        assertThat(result.items()).singleElement()
            .extracting("title").isEqualTo("지난달 과오결제 취소 요청");
    }

    @Test
    void 분석_전이면_제목은_null이다() {
        Call call = call();
        when(callRepository.searchReviews(any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(call)));
        when(analysisResultRepository.findByCallIdIn(List.of(call.getId()))).thenReturn(List.of());

        ReviewListResponse result = reviewQueryService.list(UUID.randomUUID(), null, null, null, 0, 20);

        assertThat(result.items()).singleElement().extracting("title").isNull();
    }

    @Test
    void 부분_실패로_판정이_비어도_목록이_뜬다() {
        // Ollama가 꺼지면 grade·summary·needsFollowup이 전부 null로 들어온다 (부분 실패 허용).
        // 이때도 대기열은 떠야 한다 — 예전엔 여기서 목록 API 전체가 NPE로 죽었다.
        Call call = call();
        when(callRepository.searchReviews(any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(call)));
        when(analysisResultRepository.findByCallIdIn(List.of(call.getId())))
            .thenReturn(List.of(AnalysisResult.builder().callId(call.getId()).build()));

        ReviewListResponse result = reviewQueryService.list(UUID.randomUUID(), null, null, null, 0, 20);

        assertThat(result.items()).singleElement()
            .satisfies(item -> {
                assertThat(item.title()).isNull();
                assertThat(item.highConfidenceFieldCount()).isZero();
                assertThat(item.totalFieldCount()).isEqualTo(5);
            });
    }

    @Test
    void 관리자_전체_목록에는_상담사와_분류가_포함된다() {
        UUID counselorId = UUID.randomUUID();
        Call call = Call.builder()
            .counselorId(counselorId).audioPath("a.wav").sourceRequestId("SRC-ADMIN-1").build();
        ReflectionTestUtils.setField(call, "id", UUID.randomUUID());
        call.transitionTo(CallStatus.CONFIRMED);
        AnalysisResult analysis = AnalysisResult.builder()
            .callId(call.getId())
            .consultationType(Judgment.of("사용법문의", 0.9, List.of(0)))
            .consultationCategory(Judgment.of("시스템 이용", 0.9, List.of(0)))
            .consultationSubcategory(Judgment.of("로그인", 0.9, List.of(0)))
            .grade(Judgment.of("일반", 0.9, List.of(0)))
            .build();
        analysis.confirm(counselorId, List.of(), 83);
        Counselor counselor = Counselor.builder()
            .loginId("agent1").passwordHash("hash").name("김상담").build();
        ReflectionTestUtils.setField(counselor, "id", counselorId);

        when(callRepository.searchAdminReviews(
            eq("CONFIRMED"), any(), any(), eq("일반"), eq("사용법문의"),
            eq(counselorId.toString()), eq("홍길동"),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(call)));
        when(analysisResultRepository.findByCallIdIn(List.of(call.getId()))).thenReturn(List.of(analysis));
        when(counselorRepository.findAllById(List.of(counselorId))).thenReturn(List.of(counselor));

        ReviewListResponse result = reviewQueryService.listAll(
            CallStatus.CONFIRMED, null, null, "일반", "사용법문의", counselorId, "홍길동", 0, 50);

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.counselor()).isNotNull();
            assertThat(item.counselor().name()).isEqualTo("김상담");
            assertThat(item.classification()).isNotNull();
            assertThat(item.classification().type().value()).isEqualTo("사용법문의");
            assertThat(item.classification().category().value()).isEqualTo("시스템 이용");
            assertThat(item.classification().subcategory().value()).isEqualTo("로그인");
            assertThat(item.reviewSec()).isEqualTo(83);
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
