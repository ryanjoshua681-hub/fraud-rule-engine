package com.ryanm.fraudruleengine.service;

import com.ryanm.fraudruleengine.controller.v1.dto.FraudFlagResponse;
import com.ryanm.fraudruleengine.exception.FraudFlagNotFoundException;
import com.ryanm.fraudruleengine.mapper.FraudFlagMapper;
import com.ryanm.fraudruleengine.persistence.entity.FraudFlagEntity;
import com.ryanm.fraudruleengine.persistence.entity.RuleEvaluationEntity;
import com.ryanm.fraudruleengine.persistence.repository.FraudFlagRepository;
import com.ryanm.fraudruleengine.persistence.repository.RuleEvaluationRepository;
import com.ryanm.fraudruleengine.rule.FraudDecision;
import com.ryanm.fraudruleengine.rule.RuleResult;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FraudFlagService {

    private final FraudFlagRepository fraudFlagRepository;
    private final RuleEvaluationRepository ruleEvaluationRepository;
    private final FraudFlagMapper fraudFlagMapper;

    public FraudFlagService(
            final FraudFlagRepository fraudFlagRepository,
            final RuleEvaluationRepository ruleEvaluationRepository,
            final FraudFlagMapper fraudFlagMapper) {
        this.fraudFlagRepository = fraudFlagRepository;
        this.ruleEvaluationRepository = ruleEvaluationRepository;
        this.fraudFlagMapper = fraudFlagMapper;
    }

    public void save(final FraudDecision decision) {
        final String triggeredRules = decision.ruleResults().stream()
                .filter(r -> !(r instanceof RuleResult.Allow))
                .map(RuleResult::ruleName)
                .collect(Collectors.joining(","));

        final FraudFlagEntity flag = FraudFlagEntity.builder()
                .transactionId(decision.transactionId())
                .riskScore(decision.riskScore())
                .riskLevel(decision.riskLevel())
                .actionTaken(decision.action())
                .triggeredRules(triggeredRules)
                .build();

        final FraudFlagEntity saved = fraudFlagRepository.save(flag);

        decision.ruleResults().forEach(result -> {
            final RuleEvaluationEntity eval = buildEvaluationEntity(saved.getId(), result);
            ruleEvaluationRepository.save(eval);
        });
    }

    @Transactional(readOnly = true)
    public FraudFlagResponse findByTransactionId(final UUID transactionId) {
        return fraudFlagRepository
                .findByTransactionId(transactionId)
                .map(fraudFlagMapper::toResponse)
                .orElseThrow(() -> new FraudFlagNotFoundException(transactionId));
    }

    private RuleEvaluationEntity buildEvaluationEntity(final UUID fraudFlagId, final RuleResult result) {
        final String resultType =
                switch (result) {
                    case RuleResult.Allow ignored -> "ALLOW";
                    case RuleResult.Flag ignored -> "FLAG";
                    case RuleResult.Block ignored -> "BLOCK";
                };
        final int score = result instanceof RuleResult.Flag f ? f.score() : 0;
        final String reason =
                switch (result) {
                    case RuleResult.Allow ignored -> null;
                    case RuleResult.Flag f -> f.reason();
                    case RuleResult.Block b -> b.reason();
                };
        return RuleEvaluationEntity.builder()
                .fraudFlagId(fraudFlagId)
                .ruleName(result.ruleName())
                .resultType(resultType)
                .score(score)
                .reason(reason)
                .build();
    }
}
