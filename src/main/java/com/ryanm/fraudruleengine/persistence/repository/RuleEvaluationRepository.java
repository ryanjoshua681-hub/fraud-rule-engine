package com.ryanm.fraudruleengine.persistence.repository;

import com.ryanm.fraudruleengine.persistence.entity.RuleEvaluationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleEvaluationRepository extends CrudRepository<RuleEvaluationEntity, UUID> {

    List<RuleEvaluationEntity> findByFraudFlagId(UUID fraudFlagId);
}
