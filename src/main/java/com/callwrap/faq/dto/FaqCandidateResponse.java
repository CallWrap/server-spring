package com.callwrap.faq.dto;

import java.util.UUID;

/** FastAPI가 소분류별로 묶어 EXAONE에 넘길 원재료 1건. resolution 없이는 EXAONE이 답변을 지어내야 해서 같이 넘긴다. */
public record FaqCandidateResponse(
    UUID callId,
    String subcategory,
    String inquiry,
    String resolution
) {
}
