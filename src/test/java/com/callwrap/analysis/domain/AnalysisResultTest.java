package com.callwrap.analysis.domain;

import com.callwrap.global.common.Judgment;
import com.callwrap.global.exception.CallWrapException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 확정(confirm)이 요약의 구조를 지키는지. 평문 한 덩어리로 덮어쓰면 대기열 제목과
 * FAQ 후보 조회가 조용히 죽는다 — 조용히 망가지느니 거절해야 한다.
 */
class AnalysisResultTest {

    @Test
    void 요약_확정값이_구조를_유지한다() {
        AnalysisResult analysis = withSummary();

        analysis.applyConfirmedFields(Map.of("summary", Map.of(
            "title", "본인부담금 처리 문의",
            "inquiry", "수급자가 본인부담금 처리 여부를 문의함",
            "resolution", "가상계좌 입금 확인 후 하루 소요를 안내함")));

        assertThat(analysis.getSummary().value()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) analysis.getSummary().value()).get("title"))
            .isEqualTo("본인부담금 처리 문의");
        assertThat(analysis.getSummary().confidence()).isEqualTo(1.0);
    }

    @Test
    void 요약을_평문으로_보내면_거절한다() {
        AnalysisResult analysis = withSummary();
        Map<String, Object> flattened = Map.of("summary", "[제목]\n문의: ...\n처리: ...");

        assertThatThrownBy(() -> analysis.applyConfirmedFields(flattened))
            .isInstanceOf(CallWrapException.class);
    }

    @Test
    void 구조가_불완전해도_거절한다() {
        AnalysisResult analysis = withSummary();
        Map<String, Object> partial = Map.of("summary", Map.of("title", "제목만 있음"));

        assertThatThrownBy(() -> analysis.applyConfirmedFields(partial))
            .isInstanceOf(CallWrapException.class);
    }

    private AnalysisResult withSummary() {
        return AnalysisResult.builder()
            .callId(UUID.randomUUID())
            .summary(Judgment.of(Map.of("title", "원본", "inquiry", "원본 문의", "resolution", "원본 처리"),
                0.88, List.of(1)))
            .build();
    }
}
