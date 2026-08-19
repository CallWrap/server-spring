package com.callwrap.review.controller;

import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.dto.ReviewConfirmRequest;
import com.callwrap.review.dto.ReviewConfirmResponse;
import com.callwrap.review.dto.ReviewCreateRequest;
import com.callwrap.review.dto.ReviewCreateResponse;
import com.callwrap.review.dto.ReviewDetailResponse;
import com.callwrap.review.dto.ReviewListResponse;
import com.callwrap.review.dto.ReviewStatsResponse;
import com.callwrap.review.service.ReviewConfirmService;
import com.callwrap.review.service.ReviewCreateService;
import com.callwrap.review.service.ReviewQueryService;
import com.callwrap.review.service.ReviewStatsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/*
 * 컨트롤러는 검증·위임 - 비즈니스 로직은 각 단일책임 서비스에
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewCreateService reviewCreateService;
    private final ReviewQueryService reviewQueryService;
    private final ReviewConfirmService reviewConfirmService;
    private final ReviewStatsService reviewStatsService;

    @Operation(summary = "검토 생성", description = "사보원이 상담 종료 시 음성파일 URL + 상담사 정보를 넘겨 검토를 생성한다. 즉시 202로 응답하고 처리는 큐 뒤에서 진행된다.")
    @PostMapping
    public ResponseEntity<ReviewCreateResponse> create(@Valid @RequestBody ReviewCreateRequest request) {
        ReviewCreateResponse response = reviewCreateService.create(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "검토 목록 조회", description = "상태 필터 + 페이지네이션. 목록에는 요약 정보만 내려간다.")
    @GetMapping
    public ReviewListResponse list(
            @RequestParam(name = "status", required = false) CallStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reviewQueryService.list(status, page, size);
    }

    @Operation(summary = "검토 상세 조회")
    @GetMapping("/{reviewId}")
    public ReviewDetailResponse detail(@PathVariable UUID reviewId) {
        return reviewQueryService.getDetail(reviewId);
    }

    @Operation(summary = "관리자 대시보드 집계", description = "상태별·등급별·유형별 건수와 평균 검토 시간을 DB에서 " +
        "GROUP BY로 집계해서 반환한다. 목록 API로 콜을 가져와 프론트에서 세는 대신 이 엔드포인트를 쓴다 — " +
        "목록은 page/size로 잘려 있어서 그렇게 세면 전체 건수를 넘는 순간부터 통계가 틀어진다.")
    @GetMapping("/stats")
    public ReviewStatsResponse stats() {
        return reviewStatsService.getStats();
    }

    @Operation(summary = "검토 확정", description = "상담사가 AI 분석 결과를 검토 후 확정한다. JWT 인증 필요.")
    @PatchMapping("/{reviewId}/confirm")
    public ReviewConfirmResponse confirm(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UUID counselorId,
            @Valid @RequestBody ReviewConfirmRequest request
    ) {
        return reviewConfirmService.confirm(reviewId, counselorId, request);
    }
}
