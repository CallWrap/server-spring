package com.callwrap.review.controller;

import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.dto.ReviewConfirmRequest;
import com.callwrap.review.dto.ReviewConfirmResponse;
import com.callwrap.review.dto.ReviewCreateRequest;
import com.callwrap.review.dto.ReviewCreateResponse;
import com.callwrap.review.dto.ReviewDetailResponse;
import com.callwrap.review.dto.ReviewListResponse;
import com.callwrap.review.service.ReviewConfirmService;
import com.callwrap.review.service.ReviewCreateService;
import com.callwrap.review.service.ReviewQueryService;
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

/**
 * 컨트롤러는 얇게 유지한다 — 검증·위임만 하고 비즈니스 로직은 각 단일책임 서비스에 둔다.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewCreateService reviewCreateService;
    private final ReviewQueryService reviewQueryService;
    private final ReviewConfirmService reviewConfirmService;

    @PostMapping
    public ResponseEntity<ReviewCreateResponse> create(@Valid @RequestBody ReviewCreateRequest request) {
        ReviewCreateResponse response = reviewCreateService.create(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    public ReviewListResponse list(
            @RequestParam(name = "status", required = false) CallStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reviewQueryService.list(status, page, size);
    }

    @GetMapping("/{reviewId}")
    public ReviewDetailResponse detail(@PathVariable UUID reviewId) {
        return reviewQueryService.getDetail(reviewId);
    }

    @PatchMapping("/{reviewId}/confirm")
    public ReviewConfirmResponse confirm(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UUID counselorId,
            @Valid @RequestBody ReviewConfirmRequest request
    ) {
        return reviewConfirmService.confirm(reviewId, counselorId, request);
    }
}
