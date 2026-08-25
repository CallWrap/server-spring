package com.callwrap.faq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/*
 * FAQ 배치 생성을 매일 새벽 4시(KST)에 자동으로 돌린다. 상담사 실사용 시간대를 피해
 * FastAPI·EXAONE 자원 경합을 줄인다. 관리자 화면의 수동 트리거(FaqController → FaqBatchTriggerService)는
 * 그대로 남겨둔다 — 데모 중 즉시 결과를 보고 싶을 때, 또는 새벽 배치가 실패했을 때 재시도 용도로 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FaqBatchScheduler {

    private final FaqBatchTriggerService faqBatchTriggerService;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void runNightlyBatch() {
        log.info("[faq] 새벽 4시 자동 배치 생성 시작");
        try {
            Map<String, Object> result = faqBatchTriggerService.triggerGenerateBatch();
            log.info("[faq] 자동 배치 생성 완료: {}", result);
        } catch (Exception e) {
            // 스케줄러 스레드에서 예외가 밖으로 나가면 이후 스케줄이 흔들릴 수 있어 여기서 잡는다.
            // 실패해도 다음날 새벽에 다시 시도되고, 그 사이엔 기존 FAQ가 그대로 남아 화면이 비지 않는다.
            log.error("[faq] 자동 배치 생성 실패", e);
        }
    }
}
