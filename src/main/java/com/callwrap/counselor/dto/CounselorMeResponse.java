package com.callwrap.counselor.dto;

import java.util.UUID;

public record CounselorMeResponse(UUID counselorId, String name, String loginId) {
}
