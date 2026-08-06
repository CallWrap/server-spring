package com.callwrap.pipeline.messaging;

import com.callwrap.review.service.ResultProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

//결과 큐&수신 지점입니다.
//리스너를 얇게 유지하기 위해서 실제 판단/저장은 ResultProcessingService에 위임하는 형태로
//구현하였습니다.

@Slf4j
@Component
@RequiredArgsConstructor
public class ResultMessageListener {

    private final ResultProcessingService resultProcessingService;

    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    public void onResult(ResultMessage resultMessage) {
        log.info("[pipeline] 결과 수신 callId={} status={}", resultMessage.callId(), resultMessage.status());
        resultProcessingService.process(resultMessage);
    }
}