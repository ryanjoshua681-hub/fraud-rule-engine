package com.ryanm.fraudruleengine.rule.impl;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.rule.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoundAmountRuleTest {

    private RoundAmountRule rule;

    @BeforeEach
    void setUp() {
        final ApplicationConfigurationProperties config = new ApplicationConfigurationProperties();
        // default roundAmountThreshold=10_000
        rule = new RoundAmountRule(config);
    }

    @Test
    void givenSmallRoundAmount_whenEvaluate_expectAllow() {
        // Below threshold — round amounts are fine at small values
        final Transaction tx = transaction(BigDecimal.valueOf(5_000));
        assertThat(rule.evaluate(tx)).isInstanceOf(RuleResult.Allow.class);
    }

    @Test
    void givenLargeNonRoundAmount_whenEvaluate_expectAllow() {
        final Transaction tx = transaction(BigDecimal.valueOf(15_750));
        assertThat(rule.evaluate(tx)).isInstanceOf(RuleResult.Allow.class);
    }

    @Test
    void givenLargeRoundAmount_whenEvaluate_expectFlag() {
        final Transaction tx = transaction(BigDecimal.valueOf(50_000));
        final RuleResult result = rule.evaluate(tx);
        assertThat(result).isInstanceOf(RuleResult.Flag.class);
        assertThat(((RuleResult.Flag) result).score()).isEqualTo(25);
    }

    @Test
    void givenLargeRoundAmountAtThreshold_whenEvaluate_expectAllow() {
        // Exactly at threshold — not strictly greater than, so allow
        final Transaction tx = transaction(BigDecimal.valueOf(10_000));
        assertThat(rule.evaluate(tx)).isInstanceOf(RuleResult.Allow.class);
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
