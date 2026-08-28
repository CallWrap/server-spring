package com.callwrap.faq.repository;

import com.callwrap.faq.domain.FaqEntry;
import com.callwrap.faq.domain.FaqStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaqEntryRepository extends JpaRepository<FaqEntry, UUID> {

    // 근거 콜이 많이 쌓인(=많이 문의된) FAQ부터 보이게 정렬한다 — 검수하는 관리자에게 우선순위를
    // "추천"하는 신호로 쓴다. 같은 건수면 최근 생성분을 위로.
    List<FaqEntry> findByStatusOrderBySourceCallCountDescGeneratedAtDesc(FaqStatus status);

    List<FaqEntry> findAllByOrderBySourceCallCountDescGeneratedAtDesc();

    Optional<FaqEntry> findByQuestion(String question);

    /** 병합 대상 탐색용 — 반려(DISCARDED)는 대상에서 뺀다. 상담사가 명시적으로 거절한 걸
     * 배치가 조용히 되살리면 그 판단을 무시하는 셈이 되기 때문. */
    List<FaqEntry> findBySubcategoryAndStatusIn(String subcategory, List<FaqStatus> statuses);
}
