package com.callwrap.review.domain;

public enum CallStatus {
    UPLOADED,
    PROCESSING,
    NEEDS_HUMAN_REVIEW,
    DONE,
    FAILED,
    CONFIRMED;

    public boolean isTerminal() {
        return this == CONFIRMED || this == FAILED;
    }
}
