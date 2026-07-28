package com.callwrap.counselor.controller;

import com.callwrap.counselor.dto.CounselorMeResponse;
import com.callwrap.counselor.service.CounselorQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor

@Tag(name = "상담사(Counselor)", description = "상담사 본인 정보 조회")
public class CounselorController {

    private final CounselorQueryService counselorQueryService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public CounselorMeResponse me(@AuthenticationPrincipal UUID counselorId) {
        return counselorQueryService.getMe(counselorId);
    }
}
