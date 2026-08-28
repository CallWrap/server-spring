package com.callwrap.faq.controller;

import com.callwrap.faq.domain.FaqStatus;
import com.callwrap.faq.dto.FaqEntryResponse;
import com.callwrap.faq.dto.FaqEntryUpdateRequest;
import com.callwrap.faq.dto.FaqRevisionResponse;
import com.callwrap.faq.dto.FaqSimilarResponse;
import com.callwrap.faq.service.FaqBatchTriggerService;
import com.callwrap.faq.service.FaqEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * 상담사/관리자 검수용. EXAONE이 만든 DRAFT를 수정·승인(PUBLISHED)·반려(DISCARDED)한다.
 * 컨트롤러는 검증·위임만 — ReviewController와 동일한 원칙.
 */
@RestController
@RequestMapping("/api/faq-entries")
@RequiredArgsConstructor
@Tag(name = "FAQ 검수", description = "EXAONE이 생성한 FAQ 초안을 상담사가 검토·승인한다")
public class FaqController {

    private final FaqEntryService faqEntryService;
    private final FaqBatchTriggerService faqBatchTriggerService;

    @Operation(summary = "FAQ 배치 생성 트리거", description = "FastAPI에 needs_faq 후보 → EXAONE 합성을 요청한다. 관리자가 수동으로 누르는 동작이라 시간이 걸릴 수 있다.")
    @PostMapping("/generate-batch")
    public Map<String, Object> generateBatch() {
        return faqBatchTriggerService.triggerGenerateBatch();
    }

    @Operation(summary = "FAQ 목록 조회", description = "status 필터 없으면 전체(DRAFT+PUBLISHED+DISCARDED) 반환.")
    @GetMapping
    public List<FaqEntryResponse> list(@RequestParam(required = false) FaqStatus status) {
        return faqEntryService.list(status);
    }

    @Operation(summary = "FAQ 문항 수정", description = "검수 중 질문/답변을 다듬는다.")
    @PatchMapping("/{id}")
    public FaqEntryResponse update(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID counselorId,
            @Valid @RequestBody FaqEntryUpdateRequest request) {
        return faqEntryService.update(id, counselorId, request);
    }

    @Operation(summary = "유사 FAQ 조회", description = "같은 소분류의 기존 FAQ를 질문 유사도 높은 순으로 반환한다.")
    @GetMapping("/{id}/similar")
    public List<FaqSimilarResponse> similar(@PathVariable UUID id) {
        return faqEntryService.similar(id);
    }

    @Operation(summary = "기존 FAQ로 병합", description = "기존 FAQ의 이전 내용을 이력으로 보관하고 초안 내용으로 개정한다.")
    @PostMapping("/{draftId}/merge-into/{targetId}")
    public FaqEntryResponse mergeInto(
            @PathVariable UUID draftId,
            @PathVariable UUID targetId,
            @AuthenticationPrincipal UUID counselorId) {
        return faqEntryService.mergeInto(draftId, targetId, counselorId);
    }

    @Operation(summary = "FAQ 개정 이력", description = "수정 전 질문·답변과 수정자·수정시각을 최신순으로 반환한다.")
    @GetMapping("/{id}/revisions")
    public List<FaqRevisionResponse> revisions(@PathVariable UUID id) {
        return faqEntryService.revisions(id);
    }

    @Operation(summary = "FAQ 승인", description = "DRAFT → PUBLISHED. JWT 인증 필요.")
    @PostMapping("/{id}/publish")
    public FaqEntryResponse publish(@PathVariable UUID id, @AuthenticationPrincipal UUID counselorId) {
        return faqEntryService.publish(id, counselorId);
    }

    @Operation(summary = "FAQ 반려", description = "부정확하거나 부적절한 초안을 폐기한다. JWT 인증 필요.")
    @PostMapping("/{id}/discard")
    public FaqEntryResponse discard(@PathVariable UUID id, @AuthenticationPrincipal UUID counselorId) {
        return faqEntryService.discard(id, counselorId);
    }
}
