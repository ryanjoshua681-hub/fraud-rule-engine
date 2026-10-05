package com.ryanm.fraudruleengine.rule.impl;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.rule.FraudRule;
import com.ryanm.fraudruleengine.rule.RuleResult;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RoundAmountRule implements FraudRule {

    private static final BigDecimal ROUND_DIVISOR = BigDecimal.valueOf(1000);

    private final ApplicationConfigurationProperties config;

    public RoundAmountRule(final ApplicationConfigurationProperties config) {
        this.config = config;
    }

    @Override
    public RuleResult evaluate(final Transaction transaction) {
        final BigDecimal amount = transaction.getAmount();
        final BigDecimal threshold = config.getRules().getRoundAmountThreshold();

        final boolean suspiciouslyRound = amount.compareTo(threshold) > 0
                && amount.remainder(ROUND_DIVISOR).compareTo(BigDecimal.ZERO) == 0;

        if (suspiciouslyRound) {
            return new RuleResult.Flag(getName(), 25, "Suspiciously round large amount: " + amount);
        }
        return new RuleResult.Allow(getName());
    }

    @Override
    public String getName() {
        return "ROUND_AMOUNT_RULE";
    }

    @Override
    public int getWeight() {
        return 25;
    }
}
