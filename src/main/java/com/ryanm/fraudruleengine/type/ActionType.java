package com.ryanm.fraudruleengine.type;

public enum ActionType {
    SUBMITTED,
    EVALUATED,
    ALLOWED,
    FLAGGED,
    BLOCKED,
    DLQ_RETRY
}
