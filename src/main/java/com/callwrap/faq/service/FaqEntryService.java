package com.callwrap.faq.service;

import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.faq.domain.FaqEntry;
import com.callwrap.faq.domain.FaqStatus;
import com.callwrap.faq.dto.FaqEntryCreateRequest;
import com.callwrap.faq.dto.FaqEntryResponse;
import com.callwrap.faq.dto.FaqEntryUpdateRequest;
import com.callwrap.faq.dto.FaqRevisionResponse;
import com.callwrap.faq.dto.FaqSimilarResponse;
import com.callwrap.faq.domain.FaqEntryRevision;
import com.callwrap.faq.repository.FaqEntryRepository;
import com.callwrap.faq.repository.FaqEntryRevisionRepository;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FaqEntryService {

    /** 병합 대상으로 볼 상태 — 반려(DISCARDED)는 제외한다. */
    private static final List<FaqStatus> MERGEABLE_STATUSES = List.of(FaqStatus.DRAFT, FaqStatus.PUBLISHED);

    /** 질문 문장 유사도가 이 값 이상이면 "같은 FAQ"로 보고 병합한다. 형태소 분석 없는 단순 단어
     * 겹침 비율(Jaccard)이라 조사 차이 등으로 실제보다 낮게 나올 수 있다 — 그래도 이 판정 자체는
     * LLM 호출 없이 계산 가능해야 한다는 원칙(붙이는 판정은 언어 추론이 필요 없다, EXAONE은
     * 문항 합성에만 쓴다)에 맞춰 이 수준에서 둔다. */
    private static final double SIMILARITY_THRESHOLD = 0.5;

    private final FaqEntryRepository faqEntryRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final FaqEntryRevisionRepository faqEntryRevisionRepository;

    /**
     * FastAPI가 EXAONE 배치 생성 결과 1건을 넘길 때 호출된다. 같은 소분류 + 질문 문장이 비슷한
     * 기존 DRAFT/PUBLISHED가 있으면 새로 만들지 않고 그 항목에 근거만 누적한다(병합) — 매 배치마다
     * 같은 주제의 FAQ가 중복으로 쌓이는 걸 막는다. 없으면 새 DRAFT를 만든다. 어느 경로든 재료로
     * 쓰인 콜은 faq_processed_at으로 마킹해서 다음 배치에 중복 재사용되지 않게 한다.
     */
    @Transactional
    public FaqEntryResponse create(FaqEntryCreateRequest request) {
        FaqEntry entry = findMergeTarget(request.subcategory(), request.question())
            .map(existing -> {
                existing.absorb(request.sourceCallCount(), request.sampleCallIds());
                return existing;
            })
            .orElseGet(() -> {
                FaqEntry fresh = FaqEntry.draft(
                    request.subcategory(), request.question(), request.answer(),
                    request.sourceCallCount(), request.sampleCallIds());
                faqEntryRepository.save(fresh);
                return fresh;
            });

        analysisResultRepository.findByCallIdIn(request.sampleCallIds())
            .forEach(ar -> ar.markFaqProcessed());

        return toResponse(entry);
    }

    /**
     * 같은 소분류(①) 안에서 질문 문장이 충분히 비슷한(②) 기존 항목을 찾는다. 소분류만으로는
     * 부족한 게, 한 소분류 안에 서로 다른 FAQ 여러 개가 있을 수 있어서(예: "자료확인" 소분류에
     * "환불 자료 확인 방법"과 "가입 자료 확인 방법"이 따로 있을 수 있음) — 그중 정말 같은 걸
     * 물어본 건지는 문장을 봐야 한다. LLM을 안 쓰는 이유는 이 판정 자체가 언어 추론이 아니라
     * "겹치는가 아닌가"만 보면 되는 매칭 작업이기 때문 — 언어 추론이 필요한 합성만 EXAONE에
     * 맡기고 나머지는 코드로 처리해서 불필요한 LLM 호출을 줄인다.
     */
    private Optional<FaqEntry> findMergeTarget(String subcategory, String question) {
        return faqEntryRepository.findBySubcategoryAndStatusIn(subcategory, MERGEABLE_STATUSES).stream()
            .map(e -> Map.entry(e, wordOverlapSimilarity(e.getQuestion(), question)))
            .filter(e -> e.getValue() >= SIMILARITY_THRESHOLD)
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);
    }

    /** 형태소 분석 없는 단순 단어(공백 기준) 겹침 비율(Jaccard) — LLM 없이 "문장이 얼마나
     * 비슷한가"를 근사한다. 조사가 붙어 같은 단어가 다르게 토큰화되는 등 정확하진 않다. */
    private double wordOverlapSimilarity(String a, String b) {
        Set<String> tokensA = tokenize(a);
        Set<String> tokensB = tokenize(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);
        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(text.trim().split("\\s+")));
    }

    @Transactional(readOnly = true)
    public List<FaqEntryResponse> list(FaqStatus status) {
        // 근거 콜이 많이 쌓인(=많이 문의된) FAQ부터 보이게 정렬한다 — 관리자에게 우선순위를
        // "추천"하는 신호로 쓴다.
        var entries = status != null
            ? faqEntryRepository.findByStatusOrderBySourceCallCountDescGeneratedAtDesc(status)
            : faqEntryRepository.findAllByOrderBySourceCallCountDescGeneratedAtDesc();
        return entries.stream().map(this::toResponse).toList();
    }

    @Transactional
    public FaqEntryResponse update(UUID id, UUID counselorId, FaqEntryUpdateRequest request) {
        FaqEntry entry = getOrThrow(id);
        if (!Objects.equals(entry.getQuestion(), request.question())
            || !Objects.equals(entry.getAnswer(), request.answer())) {
            faqEntryRevisionRepository.save(FaqEntryRevision.capture(entry, counselorId));
            entry.edit(request.question(), request.answer());
        }
        return toResponse(entry);
    }

    @Transactional(readOnly = true)
    public List<FaqSimilarResponse> similar(UUID id) {
        FaqEntry source = getOrThrow(id);
        return faqEntryRepository
            .findBySubcategoryAndStatusIn(source.getSubcategory(), MERGEABLE_STATUSES).stream()
            .filter(candidate -> !candidate.getId().equals(source.getId()))
            .map(candidate -> new FaqSimilarResponse(
                candidate.getId(), candidate.getQuestion(), candidate.getSubcategory(),
                candidate.getStatus().name(), candidate.getUpdatedAt(),
                wordOverlapSimilarity(source.getQuestion(), candidate.getQuestion())))
            .sorted(java.util.Comparator.comparingDouble(FaqSimilarResponse::similarity).reversed())
            .toList();
    }

    @Transactional
    public FaqEntryResponse mergeInto(UUID draftId, UUID targetId, UUID counselorId) {
        FaqEntry draft = getOrThrow(draftId);
        FaqEntry target = getOrThrow(targetId);
        boolean invalid = draftId.equals(targetId)
            || draft.getStatus() != FaqStatus.DRAFT
            || !MERGEABLE_STATUSES.contains(target.getStatus())
            || !Objects.equals(draft.getSubcategory(), target.getSubcategory());
        if (invalid) {
            throw new CallWrapException(ErrorCode.FAQ_MERGE_INVALID);
        }

        faqEntryRevisionRepository.save(FaqEntryRevision.capture(target, counselorId));
        target.edit(draft.getQuestion(), draft.getAnswer());
        target.absorb(draft.getSourceCallCount(), draft.getSampleCallIds());
        draft.markMergedInto(targetId, counselorId);
        return toResponse(target);
    }

    @Transactional(readOnly = true)
    public List<FaqRevisionResponse> revisions(UUID id) {
        getOrThrow(id);
        return faqEntryRevisionRepository.findByFaqEntryIdOrderByRevisedAtDesc(id).stream()
            .map(revision -> new FaqRevisionResponse(
                revision.getId(), revision.getQuestion(), revision.getAnswer(),
                revision.getRevisedByCounselorId(), revision.getRevisedAt()))
            .toList();
    }

    @Transactional
    public FaqEntryResponse publish(UUID id, UUID counselorId) {
        FaqEntry entry = getOrThrow(id);
        entry.publish(counselorId);
        return toResponse(entry);
    }

    @Transactional
    public FaqEntryResponse discard(UUID id, UUID counselorId) {
        FaqEntry entry = getOrThrow(id);
        entry.discard(counselorId);
        return toResponse(entry);
    }

    private FaqEntry getOrThrow(UUID id) {
        return faqEntryRepository.findById(id)
            .orElseThrow(() -> new CallWrapException(ErrorCode.FAQ_ENTRY_NOT_FOUND));
    }

    private FaqEntryResponse toResponse(FaqEntry e) {
        return new FaqEntryResponse(
            e.getId(), e.getSubcategory(), e.getQuestion(), e.getAnswer(),
            e.getSourceCallCount(), e.getSampleCallIds(), e.getStatus().name(),
            e.getGeneratedAt(), e.getPublishedAt(), e.getUpdatedAt(), e.getMergedIntoId());
    }
}
