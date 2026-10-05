package com.ryanm.fraudruleengine.rule.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.persistence.entity.TransactionEntity;
import com.ryanm.fraudruleengine.persistence.repository.TransactionRepository;
import com.ryanm.fraudruleengine.rule.RuleResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HighFrequencyRuleTest {

    @Mock
    private TransactionRepository transactionRepository;

    private HighFrequencyRule rule;

    // Mirror the ApplicationConfigurationProperties defaults
    private static final int VELOCITY_MAX_TRANSACTIONS = 5;
    private static final int VELOCITY_WINDOW_SECONDS = 60;

    @BeforeEach
    void setUp() {
        rule = new HighFrequencyRule(transactionRepository, new ApplicationConfigurationProperties());
    }

    @Test
    void givenNoTransactionsInWindow_whenEvaluate_expectAllow() {
        when(transactionRepository.findByAccountIdAndTimestampAfter(any(), any()))
                .thenReturn(List.of());

        final RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result).isInstanceOf(RuleResult.Allow.class);
    }

    @Test
    void givenTransactionCountBelowMaximum_whenEvaluate_expectAllow() {
        when(transactionRepository.findByAccountIdAndTimestampAfter(any(), any()))
                .thenReturn(entitiesOfSize(VELOCITY_MAX_TRANSACTIONS - 1));

        final RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result).isInstanceOf(RuleResult.Allow.class);
    }

    @Test
    void givenTransactionCountAtMaximum_whenEvaluate_expectFlag() {
        // Boundary: count == max triggers a flag (>= check, not > check)
        when(transactionRepository.findByAccountIdAndTimestampAfter(any(), any()))
                .thenReturn(entitiesOfSize(VELOCITY_MAX_TRANSACTIONS));

        final RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result).isInstanceOf(RuleResult.Flag.class);
        final RuleResult.Flag flag = (RuleResult.Flag) result;
        assertThat(flag.score()).isEqualTo(50);
        assertThat(flag.ruleName()).isEqualTo("HIGH_FREQUENCY_RULE");
    }

    @Test
    void givenTransactionCountAboveMaximum_whenEvaluate_expectFlag() {
        when(transactionRepository.findByAccountIdAndTimestampAfter(any(), any()))
                .thenReturn(entitiesOfSize(VELOCITY_MAX_TRANSACTIONS + 5));

        final RuleResult result = rule.evaluate(buildTransaction());

        assertThat(result).isInstanceOf(RuleResult.Flag.class);
        assertThat(((RuleResult.Flag) result).score()).isEqualTo(50);
    }

    @Test
    void givenTransaction_whenEvaluate_expectRepositoryQueriedWithCorrectAccountIdAndWindowStart() {
        final UUID accountId = UUID.randomUUID();
        final Instant transactionTimestamp = Instant.now();
        final Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .amount(BigDecimal.valueOf(1_000))
                .currency("ZAR")
                .timestamp(transactionTimestamp)
                .build();

        final ArgumentCaptor<UUID> accountIdCaptor = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<Instant> windowStartCaptor = ArgumentCaptor.forClass(Instant.class);
        when(transactionRepository.findByAccountIdAndTimestampAfter(
                        accountIdCaptor.capture(), windowStartCaptor.capture()))
                .thenReturn(List.of());

        rule.evaluate(transaction);

        assertThat(accountIdCaptor.getValue()).isEqualTo(accountId);
        // Window start must be exactly velocityWindowSeconds before the transaction timestamp
        assertThat(windowStartCaptor.getValue()).isEqualTo(transactionTimestamp.minusSeconds(VELOCITY_WINDOW_SECONDS));
    }

    private static List<TransactionEntity> entitiesOfSize(final int size) {
        return Collections.nCopies(size, (TransactionEntity) null);
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
