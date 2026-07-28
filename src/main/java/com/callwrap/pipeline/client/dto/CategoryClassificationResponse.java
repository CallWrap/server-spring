package com.callwrap.pipeline.client.dto;

import com.callwrap.global.common.Judgment;

/** POST /classify/category 응답 — 중분류와 소분류를 한 번에 판정해서 내려준다. */
public record CategoryClassificationResponse(Judgment<Object> category, Judgment<Object> subcategory) {
}
