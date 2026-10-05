package com.ryanm.fraudruleengine.rule.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.rule.RuleResult;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class VelocityRuleTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private VelocityRule rule;

    // Mirror the ApplicationConfigurationProperties defaults
    private static final int VELOCITY_MAX_TRANSACTIONS = 5;
    private static final int VELOCITY_WINDOW_SECONDS = 60;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rule = new VelocityRule(redisTemplate, new ApplicationConfigurationProperties());
    }

    @Test
    void givenNullCountFromRedis_whenEvaluate_expectAllow() {
        when(valueOperations.increment(any())).thenReturn(null);

        final RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result).isInstanceOf(RuleResult.Allow.class);
    }

    @Test
    void givenFirstTransactionForAccount_whenEvaluate_expectAllowAndTtlIsSet() {
        final Transaction transaction = buildTransaction();
        final String expectedKey = "velocity:" + transaction.getAccountId();
        when(valueOperations.increment(expectedKey)).thenReturn(1L);

        final RuleResult result = rule.evaluate(transaction);

        assertThat(result).isInstanceOf(RuleResult.Allow.class);
        // TTL must be set on the first increment so the window eventually resets
        verify(redisTemplate).expire(expectedKey, Duration.ofSeconds(VELOCITY_WINDOW_SECONDS));
    }

    @Test
    void givenTransactionCountBelowMaximum_whenEvaluate_expectAllow() {
        when(valueOperations.increment(any())).thenReturn(3L);

        final RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result).isInstanceOf(RuleResult.Allow.class);
    }

    @Test
    void givenTransactionCountAtMaximum_whenEvaluate_expectAllow() {
        // Boundary: count == max is NOT flagged — only count > max triggers a flag
        when(valueOperations.increment(any())).thenReturn((long) VELOCITY_MAX_TRANSACTIONS);

        final RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result).isInstanceOf(RuleResult.Allow.class);
    }

    @Test
    void givenTransactionCountAboveMaximum_whenEvaluate_expectFlagWithScore70() {
        when(valueOperations.increment(any())).thenReturn((long) VELOCITY_MAX_TRANSACTIONS + 1);

        final RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result).isInstanceOf(RuleResult.Flag.class);
        final RuleResult.Flag flag = (RuleResult.Flag) result;
        assertThat(flag.score()).isEqualTo(70);
        assertThat(flag.ruleName()).isEqualTo("VELOCITY_RULE");
    }

    @Test
    void givenTransactionCountAtDoubleMaximum_whenEvaluate_expectFlagNotBlock() {
        // Boundary: count == max*2 is NOT a block — only count > max*2 triggers a block
        when(valueOperations.increment(any())).thenReturn((long) VELOCITY_MAX_TRANSACTIONS * 2);

        final RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result).isInstanceOf(RuleResult.Flag.class);
    }

    @Test
    void givenTransactionCountAboveDoubleMaximum_whenEvaluate_expectBlock() {
        when(valueOperations.increment(any())).thenReturn((long) VELOCITY_MAX_TRANSACTIONS * 2 + 1);

        final RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result).isInstanceOf(RuleResult.Block.class);
        final RuleResult.Block block = (RuleResult.Block) result;
        assertThat(block.ruleName()).isEqualTo("VELOCITY_RULE");
        assertThat(block.reason()).contains("extreme velocity");
    }

    @Test
    void givenTransactionCountAboveOne_whenEvaluate_expectTtlIsNotSetAgain() {
        final Transaction transaction = buildTransaction();
        final String expectedKey = "velocity:" + transaction.getAccountId();
        when(valueOperations.increment(expectedKey)).thenReturn(3L);

        rule.evaluate(transaction);

        // TTL is only ever set on the first increment to preserve the sliding window
        verify(redisTemplate, never()).expire(any(), any(Duration.class));
    }

    private Transaction buildTransaction() {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1_000))
                .currency("ZAR")
                .timestamp(Instant.now())
                .build();
    }
}
