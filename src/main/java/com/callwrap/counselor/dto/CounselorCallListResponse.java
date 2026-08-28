package com.callwrap.counselor.dto;

import java.util.List;

public record CounselorCallListResponse(
    List<CounselorCallResponse> items,
    int page,
    int size,
    long totalCount
) {
}
