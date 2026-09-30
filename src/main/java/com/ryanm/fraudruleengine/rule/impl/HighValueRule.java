package com.ryanm.fraudruleengine.rule.impl;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.rule.FraudRule;
import com.ryanm.fraudruleengine.rule.RuleResult;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class HighValueRule implements FraudRule {

    private final ApplicationConfigurationProperties config;

    public HighValueRule(final ApplicationConfigurationProperties config) {
        this.config = config;
    }

    @Override
    public RuleResult evaluate(final Transaction transaction) {
        final BigDecimal threshold = config.getRules().getHighValueThreshold();
        if (transaction.getAmount().compareTo(threshold) > 0) {
            return new RuleResult.Flag(
                    getName(),
                    60,
                    "Amount " + transaction.getAmount() + " exceeds threshold " + threshold);
        }
        return new RuleResult.Allow(getName());
    }

    @Override
    public String getName() {
        return "HIGH_VALUE_RULE";
    }

    @Override
    public int getWeight() {
        return 60;
    }
}
