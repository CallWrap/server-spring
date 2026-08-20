package com.callwrap.pipeline.service;

import com.callwrap.pipeline.event.CallStatusChangedEvent;
import com.callwrap.pipeline.metrics.PipelineMetrics;
import com.callwrap.review.domain.Call;
import com.callwrap.review.domain.CallStatus;
import com.callwrap.review.repository.CallRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StuckCallSweeperTest {

    @Mock
    private CallRepository callRepository;
    @Mock
    private PipelineMetrics pipelineMetrics;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private StuckCallSweeper sweeper() {
        StuckCallSweeper sweeper = new StuckCallSweeper(callRepository, pipelineMetrics, eventPublisher);
        ReflectionTestUtils.setField(sweeper, "timeoutMinutes", 15L);
        return sweeper;
    }

    @Test
    void 오래_갇힌_PROCESSING_콜을_FAILED로_내리고_이벤트를_발행한다() {
        Call stuck = call();
        when(callRepository.findByStatusAndUpdatedAtBefore(eq(CallStatus.PROCESSING), any()))
            .thenReturn(List.of(stuck));

        sweeper().sweep();

        assertThat(stuck.getStatus()).isEqualTo(CallStatus.FAILED);
        verify(eventPublisher).publishEvent(new CallStatusChangedEvent(stuck.getId(), CallStatus.FAILED));
        verify(pipelineMetrics).recordStuckCallSwept();
    }

    @Test
    void 갇힌_콜이_없으면_아무것도_하지_않는다() {
        when(callRepository.findByStatusAndUpdatedAtBefore(eq(CallStatus.PROCESSING), any()))
            .thenReturn(List.of());

        sweeper().sweep();

        verify(eventPublisher, never()).publishEvent(any());
        verify(pipelineMetrics, never()).recordStuckCallSwept();
    }

    @Test
    void 판정_기준시각은_설정한_timeout만큼_과거다() {
        when(callRepository.findByStatusAndUpdatedAtBefore(eq(CallStatus.PROCESSING), any()))
            .thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now().minusMinutes(15);

        sweeper().sweep();

        // 방금 갱신된 콜(=아직 처리 중일 수 있는 콜)이 조회 대상에 들어가면 안 된다.
        ArgumentCaptor<LocalDateTime> threshold = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(callRepository).findByStatusAndUpdatedAtBefore(eq(CallStatus.PROCESSING), threshold.capture());
        assertThat(threshold.getValue()).isBetween(before.minusSeconds(5), before.plusSeconds(5));
    }

    private Call call() {
        Call call = Call.builder()
            .counselorId(UUID.randomUUID()).audioPath("a.wav").sourceRequestId("SRC-STUCK").build();
        ReflectionTestUtils.setField(call, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(call, "updatedAt", LocalDateTime.now().minusHours(1));
        call.transitionTo(CallStatus.PROCESSING);
        return call;
    }
}
