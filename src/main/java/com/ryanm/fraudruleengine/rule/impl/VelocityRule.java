package com.ryanm.fraudruleengine.rule.impl;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.rule.FraudRule;
import com.ryanm.fraudruleengine.rule.RuleResult;
import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Uses Redis atomic counters (INCR + EXPIRE) for sub-millisecond velocity checks.
 * Each account gets a sliding counter that resets after velocityWindowSeconds.
 */
@Component
public class VelocityRule implements FraudRule {

    private final RedisTemplate<String, String> redisTemplate;
    private final ApplicationConfigurationProperties config;

    public VelocityRule(
            final RedisTemplate<String, String> redisTemplate,
            final ApplicationConfigurationProperties config) {
        this.redisTemplate = redisTemplate;
        this.config = config;
    }

    @Override
    public RuleResult evaluate(final Transaction transaction) {
        final String key = "velocity:" + transaction.getAccountId();
        final Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return new RuleResult.Allow(getName());
        }

        // Set TTL only on first increment — subsequent increments inherit the TTL
        if (count == 1) {
            redisTemplate.expire(
                    key, Duration.ofSeconds(config.getRules().getVelocityWindowSeconds()));
        }

        final int max = config.getRules().getVelocityMaxTransactions();
        if (count > (long) max * 2) {
            return new RuleResult.Block(
                    getName(),
                    count + " transactions in velocity window — extreme velocity detected");
        }
        if (count > max) {
            return new RuleResult.Flag(
                    getName(),
                    70,
                    count + " transactions in last " + config.getRules().getVelocityWindowSeconds()
                            + "s (max: " + max + ")");
        }
        return new RuleResult.Allow(getName());
    }

    @Override
    public String getName() {
        return "VELOCITY_RULE";
    }

    @Override
    public int getWeight() {
        return 70;
    }
}
