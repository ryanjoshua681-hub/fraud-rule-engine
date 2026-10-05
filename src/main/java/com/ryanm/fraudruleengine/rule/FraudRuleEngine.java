package com.ryanm.fraudruleengine.rule;

import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.type.ActionType;
import com.ryanm.fraudruleengine.type.RiskLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.StructuredTaskScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates all FraudRule implementations in parallel using Java 25 Structured Concurrency
 * (JEP 505). Each rule runs in its own virtual thread via {@code StructuredTaskScope.open()}.
 * After all subtasks complete, any failure is rethrown before results are collected, so a
 * partial set of rule results is never returned to the caller.
 *
 * <p>Rules are auto-registered via Spring's List injection — adding a new @Component FraudRule
 * automatically includes it in evaluation with no changes here.
 */
@Service
public class FraudRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(FraudRuleEngine.class);

    private final List<FraudRule> rules;

    public FraudRuleEngine(final List<FraudRule> rules) {
        this.rules = rules;
    }

    public FraudDecision evaluate(final Transaction transaction) {
        final List<RuleResult> results = runRulesInParallel(transaction);
        return buildDecision(transaction, results);
    }

    private List<RuleResult> runRulesInParallel(final Transaction transaction) {
        try (var scope = StructuredTaskScope.open()) {
            final List<StructuredTaskScope.Subtask<RuleResult>> subtasks = new ArrayList<>();
            for (final FraudRule rule : rules) {
                subtasks.add(scope.fork(() -> rule.evaluate(transaction)));
            }

            scope.join();

            for (final StructuredTaskScope.Subtask<RuleResult> subtask : subtasks) {
                if (subtask.state() == StructuredTaskScope.Subtask.State.FAILED) {
                    throw new IllegalStateException(
                            "A fraud rule failed for transaction " + transaction.getId(), subtask.exception());
                }
            }

            return subtasks.stream().map(StructuredTaskScope.Subtask::get).toList();
        } catch (final InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Rule evaluation interrupted for transaction " + transaction.getId(), interruptedException);
        }
    }

    private FraudDecision buildDecision(final Transaction transaction, final List<RuleResult> results) {
        final Optional<RuleResult.Block> block = results.stream()
                .filter(r -> r instanceof RuleResult.Block)
                .map(r -> (RuleResult.Block) r)
                .findFirst();

        if (block.isPresent()) {
            log.info(
                    "txId={} action=BLOCKED rule={} reason={}",
                    transaction.getId(),
                    block.get().ruleName(),
                    block.get().reason());
            return new FraudDecision(transaction.getId(), ActionType.BLOCKED, 100, RiskLevel.HIGH, results);
        }

        final int totalScore = results.stream()
                .filter(r -> r instanceof RuleResult.Flag)
                .mapToInt(r -> ((RuleResult.Flag) r).score())
                .sum();

        final RiskLevel riskLevel = RiskLevel.fromScore(totalScore);
        final ActionType action = riskLevel == RiskLevel.LOW ? ActionType.ALLOWED : ActionType.FLAGGED;

        log.info("txId={} action={} score={} riskLevel={}", transaction.getId(), action, totalScore, riskLevel);
        return new FraudDecision(transaction.getId(), action, totalScore, riskLevel, results);
    }
}
