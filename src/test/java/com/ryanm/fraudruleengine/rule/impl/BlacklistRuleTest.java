package com.ryanm.fraudruleengine.rule.impl;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.rule.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BlacklistRuleTest {

    private BlacklistRule rule;

    @BeforeEach
    void setUp() {
        final ApplicationConfigurationProperties config = new ApplicationConfigurationProperties();
        config.getRules().setBlacklistedMerchants(List.of("BLOCKED_MERCHANT_001", "SCAM_CORP"));
        rule = new BlacklistRule(config);
    }

    @Test
    void givenMerchantNotBlacklisted_whenEvaluate_expectAllow() {
        final Transaction tx = transaction("LEGIT_MERCHANT");
        assertThat(rule.evaluate(tx)).isInstanceOf(RuleResult.Allow.class);
    }

    @Test
    void givenBlacklistedMerchant_whenEvaluate_expectBlock() {
        final Transaction tx = transaction("BLOCKED_MERCHANT_001");
        final RuleResult result = rule.evaluate(tx);
        assertThat(result).isInstanceOf(RuleResult.Block.class);
        final RuleResult.Block block = (RuleResult.Block) result;
        assertThat(block.ruleName()).isEqualTo("BLACKLIST_RULE");
        assertThat(block.reason()).contains("BLOCKED_MERCHANT_001");
    }

    @Test
    void givenNullMerchantCode_whenEvaluate_expectAllow() {
        final Transaction tx = transaction(null);
        assertThat(rule.evaluate(tx)).isInstanceOf(RuleResult.Allow.class);
    }

    private Transaction transaction(final String merchantCode) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(500))
                .merchantCode(merchantCode)
                .currency("ZAR")
                .timestamp(Instant.now())
                .build();
    }
}
