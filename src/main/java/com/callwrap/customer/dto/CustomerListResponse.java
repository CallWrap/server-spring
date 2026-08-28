package com.callwrap.customer.dto;

import java.util.List;

public record CustomerListResponse(
    List<CustomerSummaryResponse> items,
    int page,
    int size,
    long totalCount
) {
}
