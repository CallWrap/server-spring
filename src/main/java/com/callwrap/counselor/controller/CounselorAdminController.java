package com.callwrap.counselor.controller;

import com.callwrap.counselor.dto.CounselorCallListResponse;
import com.callwrap.counselor.dto.CounselorDetailResponse;
import com.callwrap.counselor.dto.CounselorSummaryResponse;
import com.callwrap.counselor.service.CounselorAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/counselors")
@RequiredArgsConstructor
@Tag(name = "상담사 관리", description = "관리자 전용 상담사 현황·감정 지표 조회 API")
public class CounselorAdminController {

    private final CounselorAdminService counselorAdminService;

    @GetMapping
    @Operation(summary = "상담사 목록", description = "선택일 처리 집계와 최근 상태를 반환한다.")
    public List<CounselorSummaryResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return counselorAdminService.list(date != null ? date : LocalDate.now());
    }

    @GetMapping("/{counselorId}")
    @Operation(summary = "상담사 상세", description = "등급별 처리와 최근 7일 부정 발화 추이를 반환한다.")
    public CounselorDetailResponse detail(
            @PathVariable UUID counselorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return counselorAdminService.detail(counselorId, date != null ? date : LocalDate.now());
    }

    @GetMapping("/{counselorId}/calls")
    @Operation(summary = "상담사 담당 콜", description = "선택일 담당 콜과 상담사 발화 부정 점수를 반환한다.")
    public CounselorCallListResponse calls(
            @PathVariable UUID counselorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return counselorAdminService.calls(
            counselorId, date != null ? date : LocalDate.now(), page, size);
    }
}
