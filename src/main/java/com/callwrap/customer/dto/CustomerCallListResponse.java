package com.callwrap.customer.dto;

import java.util.List;

public record CustomerCallListResponse(
    List<CustomerCallResponse> items,
    int page,
    int size,
    long totalCount
) {
}
