package com.callwrap.customer.controller;

import com.callwrap.customer.dto.CustomerCallListResponse;
import com.callwrap.customer.dto.CustomerDetailResponse;
import com.callwrap.customer.dto.CustomerListResponse;
import com.callwrap.customer.service.CustomerQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "민원인 관리", description = "관리자 전용 민원인 조회 API")
public class CustomerController {

    private final CustomerQueryService customerQueryService;

    @GetMapping
    @Operation(summary = "민원인 목록", description = "이름·전화번호·소속 통합 검색과 페이지네이션을 제공한다.")
    public CustomerListResponse list(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return customerQueryService.list(query, page, size);
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "민원인 상세", description = "등급별 누계, 최근 6개월 문의 추이, 상위 소분류를 반환한다.")
    public CustomerDetailResponse detail(@PathVariable UUID customerId) {
        return customerQueryService.detail(customerId);
    }

    @GetMapping("/{customerId}/calls")
    @Operation(summary = "민원인 상담 이력", description = "담당 상담사·등급·요약·통화/검토 시간을 최신순으로 반환한다.")
    public CustomerCallListResponse calls(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return customerQueryService.calls(customerId, page, size);
    }
}
