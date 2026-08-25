package com.callwrap.faq.controller;

import com.callwrap.faq.dto.FaqCandidatesResponse;
import com.callwrap.faq.dto.FaqEntryCreateRequest;
import com.callwrap.faq.dto.FaqEntryResponse;
import com.callwrap.faq.service.FaqCandidateService;
import com.callwrap.faq.service.FaqEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * FastAPI(사보원 쪽이 아니라 우리 파이프라인)가 서비스 간 호출로 쓰는 내부 API.
 * 상담사/web은 이 경로를 쓰지 않는다 — 검수용 API는 FaqController(/api/faq-entries).
 *
 * 인증: JWT가 아니라 시스템 간 API Key(X-API-Key, ApiKeyAuthFilter)로 인증한다 —
 * SecurityConfig에서 이 경로 전체가 ApiKeyAuthFilter.SCOPE_INTERNAL 스코프로 보호된다.
 */
@RestController
@RequestMapping("/api/internal/faq")
@RequiredArgsConstructor
@Tag(name = "FAQ 내부(FastAPI 전용)", description = "FastAPI의 EXAONE 배치 생성기가 호출하는 서비스 간 API")
public class FaqInternalController {

    private final FaqCandidateService faqCandidateService;
    private final FaqEntryService faqEntryService;

    @Operation(summary = "FAQ 후보 조회", description = "needs_faq=true이고 아직 배치에 안 쓰인 콜들을 소분류·문의내용과 함께 반환한다.")
    @GetMapping("/candidates")
    public FaqCandidatesResponse candidates() {
        return faqCandidateService.getCandidates();
    }

    @Operation(summary = "FAQ 후보 등록", description = "FastAPI가 EXAONE으로 합성한 FAQ 문항 1건을 DRAFT로 저장한다. 재료로 쓰인 콜은 faq_processed_at으로 마킹돼 다음 배치에서 제외된다.")
    @PostMapping("/entries")
    public ResponseEntity<FaqEntryResponse> createEntry(@Valid @RequestBody FaqEntryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(faqEntryService.create(request));
    }
}
