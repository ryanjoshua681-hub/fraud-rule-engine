package com.ryanm.fraudruleengine.controller.v1.dto;

import com.ryanm.fraudruleengine.type.ActionType;
import com.ryanm.fraudruleengine.type.RiskLevel;
import com.ryanm.fraudruleengine.type.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        UUID accountId,
        BigDecimal amount,
        String merchantCode,
        String currency,
        Instant timestamp,
        TransactionStatus status,
        int riskScore,
        RiskLevel riskLevel,
        ActionType actionTaken,
        List<String> triggeredRules) {}
