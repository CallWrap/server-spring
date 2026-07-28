package com.callwrap.review.dto;

import java.util.List;

public record ReviewListResponse(List<ReviewSummaryResponse> items, int page, int size, long totalCount) {
}
