package com.ryanm.fraudruleengine.rule.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.rule.RuleResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HighValueRuleTest {

    private HighValueRule rule;

    @BeforeEach
    void setUp() {
        final ApplicationConfigurationProperties config = new ApplicationConfigurationProperties();
        // default threshold is 50_000
        rule = new HighValueRule(config);
    }

    @Test
    void givenAmountBelowThreshold_whenEvaluate_expectAllow() {
        final Transaction tx = transaction(BigDecimal.valueOf(10_000));
        assertThat(rule.evaluate(tx)).isInstanceOf(RuleResult.Allow.class);
    }

    @Test
    void givenAmountAtThreshold_whenEvaluate_expectAllow() {
        final Transaction tx = transaction(BigDecimal.valueOf(50_000));
        assertThat(rule.evaluate(tx)).isInstanceOf(RuleResult.Allow.class);
    }

    @Test
    void givenAmountAboveThreshold_whenEvaluate_expectFlag() {
        final Transaction tx = transaction(BigDecimal.valueOf(75_000));
        final RuleResult result = rule.evaluate(tx);
        assertThat(result).isInstanceOf(RuleResult.Flag.class);
        assertThat(((RuleResult.Flag) result).score()).isEqualTo(60);
        assertThat(((RuleResult.Flag) result).ruleName()).isEqualTo("HIGH_VALUE_RULE");
    }

    private Transaction transaction(final BigDecimal amount) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(amount)
                .currency("ZAR")
                .timestamp(Instant.now())
                .build();
    }
}
