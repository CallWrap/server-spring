package com.callwrap.counselor.service;

import com.callwrap.counselor.config.CounselorHealthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CounselorStateEvaluator {

    private final CounselorHealthProperties properties;

    public StateResult evaluate(double negativeScore, long maliciousCalls) {
        if (negativeScore >= properties.restNegativeScore()
            || maliciousCalls >= properties.restMaliciousCalls()) {
            return new StateResult("휴식 권장", reason(negativeScore, maliciousCalls));
        }
        if (negativeScore >= properties.warningNegativeScore()
            || maliciousCalls >= properties.warningMaliciousCalls()) {
            return new StateResult("주의", reason(negativeScore, maliciousCalls));
        }
        return new StateResult(
            "양호",
            String.format(Locale.ROOT, "최근 %d일 부정 발화 평균 %.0f%% · 악성콜 %d건",
                properties.lookbackDays(), negativeScore * 100, maliciousCalls));
    }

    private String reason(double negativeScore, long maliciousCalls) {
        if (negativeScore >= properties.warningNegativeScore()) {
            return String.format(Locale.ROOT, "최근 %d일 부정 발화 평균 %.0f%%",
                properties.lookbackDays(), negativeScore * 100);
        }
        return String.format(Locale.ROOT, "최근 %d일 악성콜 %d건",
            properties.lookbackDays(), maliciousCalls);
    }

    public record StateResult(String state, String reason) {
    }
}
