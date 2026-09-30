package com.ryanm.fraudruleengine.rule.impl;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.persistence.repository.TransactionRepository;
import com.ryanm.fraudruleengine.rule.FraudRule;
import com.ryanm.fraudruleengine.rule.RuleResult;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * DB-backed frequency check — complements VelocityRule's Redis counter by querying
 * persisted transactions. Catches cases where the Redis key has expired but the
 * account history remains suspicious.
 */
@Component
public class HighFrequencyRule implements FraudRule {

    private final TransactionRepository transactionRepository;
    private final ApplicationConfigurationProperties config;

    public HighFrequencyRule(
            final TransactionRepository transactionRepository,
            final ApplicationConfigurationProperties config) {
        this.transactionRepository = transactionRepository;
        this.config = config;
    }

    @Override
    public RuleResult evaluate(final Transaction transaction) {
        final Instant windowStart = transaction
                .getTimestamp()
                .minusSeconds(config.getRules().getVelocityWindowSeconds());
        final int count = transactionRepository
                .findByAccountIdAndTimestampAfter(transaction.getAccountId(), windowStart)
                .size();
        final int max = config.getRules().getVelocityMaxTransactions();
        if (count >= max) {
            return new RuleResult.Flag(
                    getName(),
                    50,
                    count + " transactions in the last "
                            + config.getRules().getVelocityWindowSeconds() + "s");
        }
        return new RuleResult.Allow(getName());
    }

    @Override
    public String getName() {
        return "HIGH_FREQUENCY_RULE";
    }

    @Override
    public int getWeight() {
        return 50;
    }
}
