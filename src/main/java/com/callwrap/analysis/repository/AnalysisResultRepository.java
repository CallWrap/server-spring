package com.callwrap.analysis.repository;

import com.callwrap.analysis.domain.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {

    Optional<AnalysisResult> findByCallId(UUID callId);

    List<AnalysisResult> findByCallIdIn(List<UUID> callIds);

    /**
     * FAQ 배치 생성 재료. needs_faq=true이고 아직 배치에 안 쓰인(faq_processed_at IS NULL) 콜만,
     * 소분류·문의내용만 뽑아서 반환한다 — FastAPI는 이 원문 텍스트를 소분류별로 묶어 EXAONE에 넘긴다.
     */
    @Query(value = """
        SELECT ar.call_id AS callId,
               ar.consultation_subcategory ->> 'value' AS subcategory,
               ar.summary -> 'value' ->> 'inquiry' AS inquiry,
               ar.summary -> 'value' ->> 'resolution' AS resolution
        FROM analysis_results ar
        WHERE ar.needs_faq ->> 'value' = 'true'
          AND ar.faq_processed_at IS NULL
          AND ar.consultation_subcategory IS NOT NULL
          AND ar.summary IS NOT NULL
        ORDER BY ar.created_at
        """, nativeQuery = true)
    List<FaqCandidateProjection> findFaqCandidates();

    /** resolution(상담사가 실제로 안내한 내용)도 같이 줘야 EXAONE이 FAQ 답변을 지어내지 않고 근거를 가진다. */
    interface FaqCandidateProjection {
        UUID getCallId();
        String getSubcategory();
        String getInquiry();
        String getResolution();
    }
}
