package com.ryanm.fraudruleengine.rule.impl;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.rule.FraudRule;
import com.ryanm.fraudruleengine.rule.RuleResult;
import org.springframework.stereotype.Component;

@Component
public class BlacklistRule implements FraudRule {

    private final ApplicationConfigurationProperties config;

    public BlacklistRule(final ApplicationConfigurationProperties config) {
        this.config = config;
    }

    @Override
    public RuleResult evaluate(final Transaction transaction) {
        final String merchantCode = transaction.getMerchantCode();
        if (merchantCode != null
                && config.getRules().getBlacklistedMerchants().contains(merchantCode)) {
            return new RuleResult.Block(getName(), "Merchant " + merchantCode + " is blacklisted");
        }
        return new RuleResult.Allow(getName());
    }

    @Override
    public String getName() {
        return "BLACKLIST_RULE";
    }

    @Override
    public int getWeight() {
        return 100;
    }
}
