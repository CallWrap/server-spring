package com.callwrap.counselor.controller;

import com.callwrap.counselor.dto.CounselorMeResponse;
import com.callwrap.counselor.service.CounselorQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class CounselorController {

    private final CounselorQueryService counselorQueryService;

    @GetMapping("/me")
    public CounselorMeResponse me(@AuthenticationPrincipal UUID counselorId) {
        return counselorQueryService.getMe(counselorId);
    }
}
