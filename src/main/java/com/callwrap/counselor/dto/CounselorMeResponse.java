package com.callwrap.counselor.dto;

import com.callwrap.counselor.domain.CounselorRole;

import java.util.UUID;

public record CounselorMeResponse(UUID counselorId, String name, String loginId, CounselorRole role) {
}
