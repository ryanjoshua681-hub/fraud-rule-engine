package com.ryanm.fraudruleengine.rule;

/**
 * Sealed hierarchy of outcomes a fraud rule can produce.
 *
 * <p>Using a sealed interface forces every switch site to handle all three cases at compile time —
 * no missed states, no nulls. Pattern matching switch reads like a specification:
 *
 * <pre>{@code
 * switch (result) {
 *     case RuleResult.Allow a  -> log.debug("Allow: {}", a.ruleName());
 *     case RuleResult.Flag  f  -> totalScore += f.score();
 *     case RuleResult.Block b  -> throw new FraudBlockException(b.reason());
 * }
 * }</pre>
 */
public sealed interface RuleResult permits RuleResult.Allow, RuleResult.Flag, RuleResult.Block {

    String ruleName();

    /** The rule ran and found no fraud signal. Contributes zero to the risk score. */
    record Allow(String ruleName) implements RuleResult {}

    /**
     * The rule detected a suspicious pattern. The score (0–100) is weighted by the rule's
     * configured weight. Multiple Flag results accumulate into a total risk score.
     */
    record Flag(String ruleName, int score, String reason) implements RuleResult {}

    /**
     * The rule detected a hard-block condition (e.g. blacklisted merchant). The transaction is
     * immediately blocked regardless of other rule scores.
     */
    record Block(String ruleName, String reason) implements RuleResult {}
}
