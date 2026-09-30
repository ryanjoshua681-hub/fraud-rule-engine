package com.ryanm.fraudruleengine.service;

import com.ryanm.fraudruleengine.controller.v1.dto.TransactionRequest;
import com.ryanm.fraudruleengine.controller.v1.dto.TransactionResponse;
import com.ryanm.fraudruleengine.exception.TransactionNotFoundException;
import com.ryanm.fraudruleengine.infrastructure.kafka.TransactionProducer;
import com.ryanm.fraudruleengine.mapper.TransactionMapper;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.persistence.entity.TransactionEntity;
import com.ryanm.fraudruleengine.persistence.repository.TransactionRepository;
import com.ryanm.fraudruleengine.rule.FraudDecision;
import com.ryanm.fraudruleengine.rule.FraudRuleEngine;
import com.ryanm.fraudruleengine.rule.RuleResult;
import com.ryanm.fraudruleengine.type.ActionType;
import com.ryanm.fraudruleengine.type.TransactionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FraudRuleEngine fraudRuleEngine;
    private final FraudFlagService fraudFlagService;
    private final TransactionMapper transactionMapper;
    private final TransactionProducer transactionProducer;

    public TransactionService(
            final TransactionRepository transactionRepository,
            final FraudRuleEngine fraudRuleEngine,
            final FraudFlagService fraudFlagService,
            final TransactionMapper transactionMapper,
            final TransactionProducer transactionProducer) {
        this.transactionRepository = transactionRepository;
        this.fraudRuleEngine = fraudRuleEngine;
        this.fraudFlagService = fraudFlagService;
        this.transactionMapper = transactionMapper;
        this.transactionProducer = transactionProducer;
    }

    public TransactionResponse evaluate(final TransactionRequest request) {
        // Build domain model: assign id and mark PENDING
        final Transaction transaction = transactionMapper.toDomain(request);
        transaction.setId(UUID.randomUUID());
        transaction.setStatus(TransactionStatus.PENDING);

        // Persist before evaluation so the DB record exists for DB-backed rules
        transactionRepository.save(transactionMapper.toEntity(transaction));

        // Evaluate all rules in parallel via StructuredTaskScope
        final FraudDecision decision = fraudRuleEngine.evaluate(transaction);

        // Update status and re-persist
        transaction.setStatus(toStatus(decision.action()));
        transactionRepository.save(transactionMapper.toEntity(transaction));

        // Persist fraud flag + per-rule evaluations
        fraudFlagService.save(decision);

        // Publish outcome to Kafka
        transactionProducer.publishDecision(decision);

        return buildResponse(transaction, decision);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(final UUID id) {
        final TransactionEntity entity = transactionRepository
                .findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        final Transaction domain = transactionMapper.toDomain(entity);
        try {
            final var flag = fraudFlagService.findByTransactionId(id);
            return new TransactionResponse(
                    domain.getId(),
                    domain.getAccountId(),
                    domain.getAmount(),
                    domain.getMerchantCode(),
                    domain.getCurrency(),
                    domain.getTimestamp(),
                    domain.getStatus(),
                    flag.riskScore(),
                    flag.riskLevel(),
                    flag.actionTaken(),
                    flag.triggeredRules());
        } catch (FraudFlagNotFoundException ignored) {
            return buildResponseNoFlag(domain);
        }
    }

    private TransactionResponse buildResponse(
            final Transaction transaction, final FraudDecision decision) {
        final List<String> triggeredRules = decision.ruleResults().stream()
                .filter(r -> !(r instanceof RuleResult.Allow))
                .map(RuleResult::ruleName)
                .toList();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getMerchantCode(),
                transaction.getCurrency(),
                transaction.getTimestamp(),
                transaction.getStatus(),
                decision.riskScore(),
                decision.riskLevel(),
                decision.action(),
                triggeredRules);
    }

    private TransactionResponse buildResponseNoFlag(final Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getMerchantCode(),
                transaction.getCurrency(),
                transaction.getTimestamp(),
                transaction.getStatus(),
                0,
                null,
                null,
                List.of());
    }

    private TransactionStatus toStatus(final ActionType action) {
        return switch (action) {
            case ALLOWED -> TransactionStatus.ALLOWED;
            case FLAGGED -> TransactionStatus.FLAGGED;
            case BLOCKED -> TransactionStatus.BLOCKED;
            default -> TransactionStatus.PENDING;
        };
    }
}
