package com.ryanm.fraudruleengine.rule.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.rule.RuleResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnusualHoursRuleTest {

    private UnusualHoursRule rule;

    @BeforeEach
    void setUp() {
        final ApplicationConfigurationProperties config = new ApplicationConfigurationProperties();
        // defaults: unusualHoursStart=23, unusualHoursEnd=5
        rule = new UnusualHoursRule(config);
    }

    @Test
    void givenTransactionAtMidday_whenEvaluate_expectAllow() {
        final Transaction tx = transactionAtHour(12);
        assertThat(rule.evaluate(tx)).isInstanceOf(RuleResult.Allow.class);
    }

    @Test
    void givenTransactionAt2am_whenEvaluate_expectFlag() {
        final Transaction tx = transactionAtHour(2);
        final RuleResult result = rule.evaluate(tx);
        assertThat(result).isInstanceOf(RuleResult.Flag.class);
        assertThat(((RuleResult.Flag) result).score()).isEqualTo(30);
    }

    @Test
    void givenTransactionAt23_whenEvaluate_expectFlag() {
        final Transaction tx = transactionAtHour(23);
        assertThat(rule.evaluate(tx)).isInstanceOf(RuleResult.Flag.class);
    }

    @Test
    void givenTransactionAt5am_whenEvaluate_expectAllow() {
        // hour == end is NOT suspicious (window is >= start OR < end)
        final Transaction tx = transactionAtHour(5);
        assertThat(rule.evaluate(tx)).isInstanceOf(RuleResult.Allow.class);
    }

    private Transaction transactionAtHour(final int hour) {
        final Instant instant = ZonedDateTime.now(ZoneId.of("Africa/Johannesburg"))
                .withHour(hour)
                .withMinute(30)
                .toInstant();
        return Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000))
                .currency("ZAR")
                .timestamp(instant)
                .build();
    }
}
