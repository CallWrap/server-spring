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

    /**
     * 관리자 대시보드 개요용 등급 분포. JSONB 값(grade->>'value')이라 네이티브 쿼리로 GROUP BY 한다 —
     * V1 마이그레이션의 표현식 인덱스((grade ->> 'value'))를 그대로 탄다. 미판정(grade IS NULL)도
     * COALESCE로 한 그룹으로 묶어서 반환한다 — 이걸 안 하면 그 행들이 label=null로 따로 잡혀서
     * Collectors.toMap에서 다루기 애매해진다.
     */
    @Query(value = """
        SELECT COALESCE(ar.grade ->> 'value', '미판정') AS label, COUNT(*) AS cnt
        FROM analysis_results ar
        GROUP BY COALESCE(ar.grade ->> 'value', '미판정')
        """, nativeQuery = true)
    List<LabelCountProjection> countByGrade();

    /** 상담유형 분포. consultation_type은 2026-08-20 SR유형(8종) 모델이 파이프라인에 병합됐다. */
    @Query(value = """
        SELECT COALESCE(ar.consultation_type ->> 'value', '미분류') AS label, COUNT(*) AS cnt
        FROM analysis_results ar
        GROUP BY COALESCE(ar.consultation_type ->> 'value', '미분류')
        """, nativeQuery = true)
    List<LabelCountProjection> countByConsultationType();

    /** 확정된 콜의 평균 검토 시간(초). 아직 확정된 콜이 없으면 null. */
    @Query(value = "SELECT AVG(ar.review_sec) FROM analysis_results ar WHERE ar.review_sec IS NOT NULL",
        nativeQuery = true)
    Double averageReviewSec();

    /** 관리자 대시보드 "상담사별 악성콜 밸런싱"용 — 상담사 이름별 악성콜 확정 건수. */
    @Query(value = """
        SELECT co.name AS name, COUNT(*) AS cnt
        FROM analysis_results ar
        JOIN calls c ON c.id = ar.call_id
        JOIN counselors co ON co.id = c.counselor_id
        WHERE ar.grade ->> 'value' = '악성콜'
        GROUP BY co.name
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<NameCountProjection> countMaliciousByCounselor();

    /**
     * 관리자 대시보드 "신뢰도" 메트릭용 — 판정 5종(유형·분류·등급·요약·후속필요)의 confidence를
     * 한 컬럼으로 펼쳐서(UNION ALL) 집계한다. ReviewQueryService.HIGH_CONFIDENCE_THRESHOLD(0.9)와
     * 같은 5개 필드·같은 기준을 쓴다 — 목록 화면의 highConfidenceFieldCount/totalFieldCount와
     * 숫자가 어긋나지 않도록. confidence가 없는(아직 판정 자체가 안 된) 필드는 집계에서 빠진다.
     */
    @Query(value = """
        SELECT
            COUNT(*) AS totalJudgedCount,
            COUNT(*) FILTER (WHERE confidence >= 0.9) AS highConfidenceCount,
            AVG(confidence) AS avgConfidence
        FROM (
            SELECT (ar.consultation_type ->> 'confidence')::float AS confidence
                FROM analysis_results ar WHERE ar.consultation_type ->> 'confidence' IS NOT NULL
            UNION ALL
            SELECT (ar.consultation_category ->> 'confidence')::float
                FROM analysis_results ar WHERE ar.consultation_category ->> 'confidence' IS NOT NULL
            UNION ALL
            SELECT (ar.grade ->> 'confidence')::float
                FROM analysis_results ar WHERE ar.grade ->> 'confidence' IS NOT NULL
            UNION ALL
            SELECT (ar.summary ->> 'confidence')::float
                FROM analysis_results ar WHERE ar.summary ->> 'confidence' IS NOT NULL
            UNION ALL
            SELECT (ar.needs_followup ->> 'confidence')::float
                FROM analysis_results ar WHERE ar.needs_followup ->> 'confidence' IS NOT NULL
        ) confidences
        """, nativeQuery = true)
    ConfidenceStatsProjection getConfidenceStats();

    interface LabelCountProjection {
        String getLabel();
        Long getCnt();
    }

    interface NameCountProjection {
        String getName();
        Long getCnt();
    }

    interface ConfidenceStatsProjection {
        Long getTotalJudgedCount();
        Long getHighConfidenceCount();
        Double getAvgConfidence();
    }
}
