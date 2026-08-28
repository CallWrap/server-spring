package com.callwrap.counselor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "callwrap.counselor-health")
public record CounselorHealthProperties(
    int lookbackDays,
    double warningNegativeScore,
    double restNegativeScore,
    int warningMaliciousCalls,
    int restMaliciousCalls
) {
}
