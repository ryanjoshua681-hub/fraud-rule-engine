package com.ryanm.fraudruleengine.controller.v1.dto;

import com.ryanm.fraudruleengine.type.ActionType;
import com.ryanm.fraudruleengine.type.RiskLevel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudFlagResponse(
        UUID id,
        UUID transactionId,
        int riskScore,
        RiskLevel riskLevel,
        ActionType actionTaken,
        List<String> triggeredRules,
        Instant createdDate) {}
