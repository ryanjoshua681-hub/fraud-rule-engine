package com.ryanm.fraudruleengine.rule.impl;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.rule.FraudRule;
import com.ryanm.fraudruleengine.rule.RuleResult;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class UnusualHoursRule implements FraudRule {

    private static final ZoneId SAST = ZoneId.of("Africa/Johannesburg");

    private final ApplicationConfigurationProperties config;

    public UnusualHoursRule(final ApplicationConfigurationProperties config) {
        this.config = config;
    }

    @Override
    public RuleResult evaluate(final Transaction transaction) {
        final int hour = transaction.getTimestamp().atZone(SAST).getHour();
        final int start = config.getRules().getUnusualHoursStart();
        final int end = config.getRules().getUnusualHoursEnd();

        // Window wraps midnight: suspicious if hour >= start OR hour < end
        final boolean suspicious = (hour >= start) || (hour < end);
        if (suspicious) {
            return new RuleResult.Flag(
                    getName(), 30, "Transaction at " + hour + ":00 SAST (window " + start + ":00–" + end + ":00)");
        }
        return new RuleResult.Allow(getName());
    }

    @Override
    public String getName() {
        return "UNUSUAL_HOURS_RULE";
    }

    @Override
    public int getWeight() {
        return 30;
    }
}
