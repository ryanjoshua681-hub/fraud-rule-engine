package com.ryanm.fraudruleengine.rule;

import com.ryanm.fraudruleengine.type.ActionType;
import com.ryanm.fraudruleengine.type.RiskLevel;
import java.util.List;
import java.util.UUID;

public record FraudDecision(
        UUID transactionId,
        ActionType action,
        int riskScore,
        RiskLevel riskLevel,
        List<RuleResult> ruleResults) {}
