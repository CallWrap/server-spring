package com.callwrap.qualitycheck.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/*
 * action은 의도적으로 String으로 둔다 (pass/retry/human_review)
 * — FastAPI 응답 JSON의 값을 그대로 저장하는 통로라, 값 형식이 Python Literal 쪽 계약과 100% 같아야 한다.
 * 서비스 코드에서 분기할 때는 {@link QualityAction#fromValue(String)}로 안전하게 변환해서 쓴다
 */

@Entity
@Table(name = "quality_check_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QualityCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "call_id", nullable = false)
    private UUID callId;

    @Column(name = "check_name", length = 100)
    private String checkName;

    private Boolean passed;

    private Double score;

    private Double threshold;

    @Column(name = "higher_is_better")
    private Boolean higherIsBetter;

    @Column(length = 30)
    private String action;

    @Column(columnDefinition = "text")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Column(name = "retry_iteration", nullable = false)
    private int retryIteration;

    @Builder
    private QualityCheckResult(UUID callId, String checkName, Boolean passed, Double score,
                                Double threshold, Boolean higherIsBetter, String action,
                                String reason, Map<String, Object> details, int retryIteration) {
        this.callId = callId;
        this.checkName = checkName;
        this.passed = passed;
        this.score = score;
        this.threshold = threshold;
        this.higherIsBetter = higherIsBetter;
        this.action = action;
        this.reason = reason;
        this.details = details;
        this.checkedAt = LocalDateTime.now();
        this.retryIteration = retryIteration;
    }
}
