package com.ryanm.fraudruleengine.rule;

import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.type.ActionType;
import com.ryanm.fraudruleengine.type.RiskLevel;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates all FraudRule implementations in parallel using Java 25 Structured Concurrency
 * (JEP 453). Each rule runs in its own virtual thread via StructuredTaskScope.ShutdownOnFailure,
 * which cancels remaining threads the moment any one fails, preventing partial results.
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
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            final List<StructuredTaskScope.Subtask<RuleResult>> subtasks = rules.stream()
                    .map(rule -> scope.fork(() -> rule.evaluate(transaction)))
                    .toList();

            scope.join().throwIfFailed();

            return subtasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Rule evaluation interrupted for transaction " + transaction.getId(), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Rule evaluation failed for transaction " + transaction.getId(), e.getCause());
        }
    }

    private FraudDecision buildDecision(
            final Transaction transaction, final List<RuleResult> results) {
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
