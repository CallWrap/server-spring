package com.callwrap.pipeline.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

//파이프라인 운영 지표를 한 곳에서만 만들도록 함

@Component
public class PipelineMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter jobPublishedCounter;
    private final Timer processingTimer;

    public PipelineMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.jobPublishedCounter = Counter.builder("callwrap_job_published_total")
            .description("발행된 JobRequest 총 개수")
            .register(meterRegistry);
        this.processingTimer = Timer.builder("callwrap_job_processing_seconds")
            .description("Job 발행부터 ResultMessage 수신까지 걸린 시간")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    public void recordJobPublished() {
        jobPublishedCounter.increment();
    }

    public void recordJobSchemaViolation() {
        meterRegistry.counter("callwrap_job_schema_violation_total").increment();
    }

    public void recordResultReceived(String status) {
        meterRegistry.counter("callwrap_result_received_total", "status", status).increment();
    }

    public void recordResultSchemaViolation(String reason) {
        meterRegistry.counter("callwrap_result_schema_violation_total", "reason", reason).increment();
    }

    /** 워커 유실로 갇혀 있다가 스위퍼가 FAILED로 내린 콜. 0이 아니면 워커 쪽을 봐야 한다는 신호다. */
    public void recordStuckCallSwept() {
        meterRegistry.counter("callwrap_stuck_call_swept_total").increment();
    }

    public void recordProcessingDuration(Duration duration) {
        processingTimer.record(duration);
    }
}
