package com.ryanm.fraudruleengine.rule;

import com.ryanm.fraudruleengine.model.Transaction;

/**
 * Strategy interface for fraud detection rules.
 *
 * <p>Each implementation encapsulates a single fraud signal. The {@link
 * com.ryanm.fraudruleengine.rule.FraudRuleEngine} composes them into a pipeline, running all rules
 * concurrently via Structured Concurrency and accumulating their scores into a final risk decision.
 *
 * <p>To add a new rule: implement this interface, annotate with {@code @Component}, and it is
 * automatically registered — no changes to the engine required.
 */
public interface FraudRule {

    /**
     * Evaluates the transaction against this rule's fraud signal.
     *
     * @param transaction the transaction under evaluation
     * @return {@link RuleResult.Allow} if no signal detected, {@link RuleResult.Flag} if suspicious,
     *     {@link RuleResult.Block} for hard-block conditions
     */
    RuleResult evaluate(Transaction transaction);

    /** Unique human-readable name for this rule, used in logs, metrics, and audit trails. */
    String getName();

    /**
     * Relative weight of this rule's score contribution (1–100). A flag from a high-weight rule
     * contributes proportionally more to the total risk score than a low-weight rule.
     */
    int getWeight();
}
