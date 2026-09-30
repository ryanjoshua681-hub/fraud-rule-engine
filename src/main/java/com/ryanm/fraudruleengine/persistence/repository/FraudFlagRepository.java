package com.ryanm.fraudruleengine.persistence.repository;

import com.ryanm.fraudruleengine.persistence.entity.FraudFlagEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudFlagRepository extends CrudRepository<FraudFlagEntity, UUID> {

    Optional<FraudFlagEntity> findByTransactionId(UUID transactionId);
}
