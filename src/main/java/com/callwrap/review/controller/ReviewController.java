package com.callwrap.review.controller;

import com.callwrap.counselor.domain.CounselorRole;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.dto.FollowupNoteCreateRequest;
import com.callwrap.review.dto.ReviewConfirmRequest;
import com.callwrap.review.dto.ReviewConfirmResponse;
import com.callwrap.review.dto.ReviewCreateRequest;
import com.callwrap.review.dto.ReviewCreateResponse;
import com.callwrap.review.dto.ReviewDetailResponse;
import com.callwrap.review.dto.ReviewListResponse;
import com.callwrap.review.dto.ReviewStatsResponse;
import com.callwrap.review.service.FollowupNoteService;
import com.callwrap.review.service.ReviewConfirmService;
import com.callwrap.review.service.ReviewCreateService;
import com.callwrap.review.service.ReviewQueryService;
import com.callwrap.review.service.ReviewStatsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
    private final FollowupNoteService followupNoteService;

    @Operation(summary = "검토 생성", description = "사보원이 상담 종료 시 음성파일 URL + 상담사 정보를 넘겨 검토를 생성한다. 즉시 202로 응답하고 처리는 큐 뒤에서 진행된다.")
    @PostMapping
    public ResponseEntity<ReviewCreateResponse> create(@Valid @RequestBody ReviewCreateRequest request) {
        ReviewCreateResponse response = reviewCreateService.create(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "검토 목록 조회", description = "로그인한 상담사 본인이 받은 콜만 나온다. 상태·기간(from~to) 필터 + 페이지네이션.")
    @GetMapping
    public ReviewListResponse list(
            @AuthenticationPrincipal UUID counselorId,
            Authentication authentication,
            @RequestParam(name = "status", required = false) CallStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean all,
            @RequestParam(defaultValue = "") String grade,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(name = "counselorId", required = false) UUID filterCounselorId,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (all) {
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> CounselorRole.ADMIN.name().equals(authority.getAuthority()));
            if (!isAdmin) {
                throw new AccessDeniedException("관리자만 전체 상담대장을 조회할 수 있습니다.");
            }
            return reviewQueryService.listAll(
                status, from, to, grade, type, filterCounselorId, query, page, size);
        }
        return reviewQueryService.list(counselorId, status, from, to, page, size);
    }

    @Operation(summary = "검토 상세 조회", description = "상담사 제한 없이 조회 가능(후속 처리 공동 작업, 민원인 이력의 과거 콜 열람을 위해).")
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

    @Operation(summary = "검토 확정", description = "상담사가 AI 분석 결과를 검토 후 확정한다. 이미 확정된 콜도 다시 호출할 수 있다(재확정) — 최초 확정 시각은 유지된다. JWT 인증 필요.")
    @PatchMapping("/{reviewId}/confirm")
    public ReviewConfirmResponse confirm(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UUID counselorId,
            @Valid @RequestBody ReviewConfirmRequest request
    ) {
        return reviewConfirmService.confirm(reviewId, counselorId, request);
    }

    @Operation(summary = "후속 처리 메모 작성", description = "날짜·내용·작성자가 쌓이는 로그. 상담사 제한 없이 누구나 작성 가능(여러 상담사가 이어받아 처리).")
    @PostMapping("/{reviewId}/followup-notes")
    public ResponseEntity<ReviewDetailResponse.FollowupNoteView> createFollowupNote(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UUID counselorId,
            @Valid @RequestBody FollowupNoteCreateRequest request
    ) {
        var view = followupNoteService.create(reviewId, counselorId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }
}
