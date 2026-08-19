package com.callwrap.faq.service;

import com.callwrap.analysis.repository.AnalysisResultRepository;
import com.callwrap.faq.domain.FaqEntry;
import com.callwrap.faq.domain.FaqStatus;
import com.callwrap.faq.dto.FaqEntryCreateRequest;
import com.callwrap.faq.dto.FaqEntryResponse;
import com.callwrap.faq.dto.FaqEntryUpdateRequest;
import com.callwrap.faq.repository.FaqEntryRepository;
import com.callwrap.global.exception.CallWrapException;
import com.callwrap.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FaqEntryService {

    private final FaqEntryRepository faqEntryRepository;
    private final AnalysisResultRepository analysisResultRepository;

    /**
     * FastAPI가 EXAONE 배치 생성 결과 1건을 넘길 때 호출된다. DRAFT로 저장하고,
     * 재료로 쓰인 콜들을 faq_processed_at으로 마킹해서 다음 배치에 중복 재사용되지 않게 한다.
     */
    @Transactional
    public FaqEntryResponse create(FaqEntryCreateRequest request) {
        FaqEntry entry = FaqEntry.draft(
            request.subcategory(), request.question(), request.answer(),
            request.sourceCallCount(), request.sampleCallIds());
        faqEntryRepository.save(entry);

        analysisResultRepository.findByCallIdIn(request.sampleCallIds())
            .forEach(ar -> ar.markFaqProcessed());

        return toResponse(entry);
    }

    @Transactional(readOnly = true)
    public List<FaqEntryResponse> list(FaqStatus status) {
        var entries = status != null
            ? faqEntryRepository.findByStatusOrderByGeneratedAtDesc(status)
            : faqEntryRepository.findAll();
        return entries.stream().map(this::toResponse).toList();
    }

    @Transactional
    public FaqEntryResponse update(UUID id, FaqEntryUpdateRequest request) {
        FaqEntry entry = getOrThrow(id);
        entry.edit(request.question(), request.answer());
        return toResponse(entry);
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
            e.getGeneratedAt(), e.getPublishedAt());
    }
}
