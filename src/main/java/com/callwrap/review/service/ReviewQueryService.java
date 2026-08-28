package com.callwrap.review.service;

import com.callwrap.analysis.domain.AnalysisResult;
import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.audio.service.AudioUploadService;
import com.callwrap.global.common.Judgment;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.domain.Complainant;
import com.callwrap.review.dto.ReviewDetailResponse;
import com.callwrap.review.dto.ReviewListResponse;
import com.callwrap.review.dto.ReviewSummaryResponse;
import com.callwrap.review.repository.CallRepository;
import com.callwrap.review.repository.ComplainantRepository;
import com.callwrap.review.repository.FollowupNoteRepository;
import com.callwrap.transcript.domain.Utterance;
import com.callwrap.transcript.repository.TranscriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/*
 * 목록 화면은 전사·분석 JSONB 전체를 안 실어보낸다 — AnalysisResult는 등급/후속필요/신뢰도
 * 집계를 뽑는 데만 쓰고(콜 id 목록으로 한 번에 배치 조회), 나머지 필드는 상세 조회에서만 편다.
 */

@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.9;
    // PostgreSQL timestamp 범위(4713 BC~294276 AD) 안에서 "사실상 무제한"인 값 — CallRepository.searchReviews
    // 주석 참고: null을 그대로 바인딩하면 파라미터 타입을 못 정해 쿼리 자체가 죽어서, 필터가 없을 때는
    // null 대신 이 sentinel을 넘긴다.
    private static final LocalDateTime MIN_DATETIME = LocalDateTime.of(1, 1, 1, 0, 0);
    private static final LocalDateTime MAX_DATETIME = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final CallRepository callRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final TranscriptRepository transcriptRepository;
    private final AudioUploadService audioUploadService;
    private final ComplainantRepository complainantRepository;
    private final FollowupNoteRepository followupNoteRepository;

    /**
     * 목록은 본인이 받은 콜만 나온다(백엔드_요청사항_상담사화면.md 5번) — counselorId는 JWT에서
     * 꺼내 컨트롤러가 넘긴다. from/to는 기간 필터(6-2번), 둘 다 없으면 전체 기간.
     */
    @Transactional(readOnly = true)
    public ReviewListResponse list(UUID counselorId, CallStatus status, LocalDate from, LocalDate to, int page, int size) {
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : MIN_DATETIME;
        // to는 그 날짜 "전체"를 포함해야 하므로 다음날 00:00 미만으로 배타적 상한을 잡는다.
        LocalDateTime toDt = to != null ? to.plusDays(1).atStartOfDay() : MAX_DATETIME;

        Page<Call> result = callRepository.searchReviews(counselorId, status, fromDt, toDt, PageRequest.of(page, size));
        List<UUID> callIds = result.getContent().stream().map(Call::getId).toList();
        Map<UUID, AnalysisResult> analysisByCallId = analysisResultRepository.findByCallIdIn(callIds).stream()
            .collect(Collectors.toMap(AnalysisResult::getCallId, a -> a));

        List<UUID> complainantIds = result.getContent().stream()
            .map(Call::getComplainantId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<UUID, Complainant> complainantById = complainantRepository.findAllById(complainantIds).stream()
            .collect(Collectors.toMap(Complainant::getId, c -> c));

        List<ReviewSummaryResponse> items = result.getContent().stream()
            .map(c -> toSummary(c, analysisByCallId.get(c.getId()), complainantById.get(c.getComplainantId())))
            .collect(Collectors.toList());
        return new ReviewListResponse(items, page, size, result.getTotalElements());
    }

    private ReviewSummaryResponse toSummary(Call call, AnalysisResult analysis, Complainant complainant) {
        // Arrays.asList다 — List.of는 null 원소에 NPE를 던진다. AI 분석은 부분 실패를 허용해서
        // (실패한 모듈의 필드는 null) 예를 들어 Ollama가 꺼져 있으면 grade·summary·needsFollowup이
        // 통째로 null로 들어오는데, List.of면 그 순간 대기열 목록 API 전체가 500으로 죽는다.
        // 아래 filter가 이미 null을 걸러내므로 담기만 하면 된다. totalFieldCount는 5로 유지된다.
        List<Judgment<Object>> reviewableFields = analysis == null ? List.of() : Arrays.asList(
            analysis.getConsultationType(), analysis.getConsultationCategory(),
            analysis.getGrade(), analysis.getSummary(), analysis.getNeedsFollowup());
        int highCount = (int) reviewableFields.stream()
            .filter(j -> j != null && j.confidence() != null && j.confidence() >= HIGH_CONFIDENCE_THRESHOLD)
            .count();
        return new ReviewSummaryResponse(
            call.getId(),
            call.getCallNo(),
            summaryTitle(analysis),
            call.getStatus().name(),
            call.getRetryCount(),
            analysis != null ? analysis.getGrade() : null,
            analysis != null ? analysis.getNeedsFollowup() : null,
            highCount,
            reviewableFields.size(),
            call.getCallStartedAt(),
            call.getCallDurationSec(),
            call.getCreatedAt(),
            call.getUpdatedAt(),
            toSummaryComplainantView(complainant));
    }

    private ReviewSummaryResponse.ComplainantView toSummaryComplainantView(Complainant complainant) {
        if (complainant == null) {
            return null;
        }
        return new ReviewSummaryResponse.ComplainantView(
            complainant.getName(), complainant.getPhone(), complainant.getOrg());
    }

    /**
     * 요약 제목. JSONB에서 올라온 Summary는 Judgment&lt;Object&gt;라 강타입이 아니라 Map이다
     * (라벨 체계가 자주 바뀌어 일부러 열어둔 설계 — Judgment 주석 참고).
     * 목록 조회는 이미 AnalysisResult를 통째로 들고 있으므로 추가 쿼리가 없다.
     */
    private String summaryTitle(AnalysisResult analysis) {
        if (analysis == null || analysis.getSummary() == null) {
            return null;
        }
        if (analysis.getSummary().value() instanceof Map<?, ?> summary
            && summary.get("title") instanceof String title
            && !title.isBlank()) {
            return title;
        }
        return null;
    }

    /**
     * 상세 조회는 상담사 제한을 두지 않는다(백엔드_요청사항_상담사화면.md 5번 "단, 상세 조회는
     * 막지 말 것") — 후속 처리를 여러 상담사가 이어받고, 민원인 이력에서 다른 상담사가 받은
     * 과거 콜로도 넘어갈 수 있어야 하기 때문이다.
     */
    @Transactional(readOnly = true)
    public ReviewDetailResponse getDetail(UUID reviewId) {
        Call call = callRepository.findById(reviewId)
            .orElseThrow(() -> new CallWrapException(ErrorCode.REVIEW_NOT_FOUND));

        AnalysisResult analysis = analysisResultRepository.findByCallId(reviewId).orElse(null);

        // 재시도가 여러 번 있었어도 화면엔 가장 최근 시도의 전사만 보여주면 된다.
        List<Utterance> transcript = transcriptRepository.findFirstByCallIdOrderByRetryIterationDesc(reviewId)
            .map(com.callwrap.transcript.domain.Transcript::getUtterances)
            .orElse(List.of());

        ReviewDetailResponse.ClassificationView classification = new ReviewDetailResponse.ClassificationView(
            analysis != null ? analysis.getConsultationType() : null,
            analysis != null ? analysis.getConsultationCategory() : null,
            analysis != null ? analysis.getConsultationSubcategory() : null,
            analysis != null ? analysis.getGrade() : null
        );

        // 저장된 audioPath는 "s3://bucket/key" 내부 참조라 브라우저가 못 연다 — 조회 시점마다
        // 짧게 살아있는 프리사인 GET URL로 바꿔서 내려준다.
        String audioUrl = audioUploadService.presignPlaybackUrl(call.getAudioPath());

        List<ReviewDetailResponse.FollowupNoteView> followupNotes = followupNoteRepository.findViewsByCallId(reviewId)
            .stream()
            .map(p -> new ReviewDetailResponse.FollowupNoteView(
                p.getId(), p.getContent(), p.getCounselorName(), p.getCreatedAt()))
            .toList();

        return new ReviewDetailResponse(
            call.getId(),
            call.getCallNo(),
            call.getStatus().name(),
            call.getCallStartedAt(),
            call.getCallDurationSec(),
            call.getCreatedAt(),
            classification,
            analysis != null ? analysis.getSummary() : null,
            analysis != null ? analysis.getNeedsFollowup() : null,
            analysis != null ? analysis.getNeedsFaq() : null,
            audioUrl,
            transcript,
            analysis != null ? analysis.getUtteranceSentiments() : List.of(),
            analysis != null ? toFailureMessages(analysis) : List.of(),
            analysis != null ? analysis.getConfirmedAt() : null,
            analysis != null ? analysis.getConfirmedByCounselorId() : null,
            toDetailComplainantView(call.getComplainantId()),
            followupNotes
        );
    }

    private ReviewDetailResponse.ComplainantView toDetailComplainantView(UUID complainantId) {
        if (complainantId == null) {
            return null;
        }
        Complainant complainant = complainantRepository.findById(complainantId).orElse(null);
        if (complainant == null) {
            return null;
        }
        Map<String, Integer> gradeCounts = analysisResultRepository.countGradesByComplainant(complainantId).stream()
            .collect(Collectors.toMap(
                AnalysisResultRepository.LabelCountProjection::getLabel,
                p -> p.getCnt().intValue()));
        return new ReviewDetailResponse.ComplainantView(
            complainant.getName(), complainant.getPhone(), complainant.getOrg(), gradeCounts);
    }

    private List<String> toFailureMessages(AnalysisResult analysis) {
        if (analysis.getFailures() == null) {
            return List.of();
        }
        // 계약서 failures는 모듈명 문자열만 준다 (reason 없음, 항상 null) — "summarize: null"처럼
        // 보이지 않도록 모듈명만 내려준다.
        return analysis.getFailures().stream()
            .map(com.callwrap.analysis.domain.PipelineFailure::stage)
            .toList();
    }
}
