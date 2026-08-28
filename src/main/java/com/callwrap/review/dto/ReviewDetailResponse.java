package com.callwrap.review.dto;

import com.callwrap.analysis.domain.UtteranceSentiment;
import com.callwrap.global.common.Judgment;
import com.callwrap.transcript.domain.Utterance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReviewDetailResponse(
    UUID reviewId,
    Integer callNo,
    String status,
    LocalDateTime callStartedAt,
    Integer callDurationSec,
    LocalDateTime createdAt,
    ClassificationView classification,
    Judgment<Object> summary,
    Judgment<Object> needsFollowup,
    Judgment<Object> needsFaq,
    String audioUrl,
    List<Utterance> transcript,
    List<UtteranceSentiment> utteranceSentiments,
    List<String> failures,
    LocalDateTime confirmedAt,
    UUID confirmedByCounselorId,
    ComplainantView complainant,
    List<FollowupNoteView> followupNotes
) {
    public record ClassificationView(
        Judgment<Object> type,
        Judgment<Object> category,
        Judgment<Object> subcategory,
        Judgment<Object> grade
    ) {
    }

    /**
     * 민원인 정보 패널(백엔드_요청사항_상담사화면.md 1번) + 상담 이력 집계(2번). gradeCounts 키는
     * 기존 /api/reviews/stats의 gradeCounts와 같은 한글 라벨("일반"/"칭찬콜"/"악성콜"/"미판정")이다 —
     * 프론트의 기존 GRADE_MAP을 그대로 재사용시키기 위함. 없으면 null(민원인 정보가 없는 콜).
     */
    public record ComplainantView(String name, String phone, String org, Map<String, Integer> gradeCounts) {
    }

    /** 후속 처리 패널 한 줄(백엔드_요청사항_상담사화면.md 4번). 최신순. */
    public record FollowupNoteView(UUID id, String content, String counselorName, LocalDateTime createdAt) {
    }
}
