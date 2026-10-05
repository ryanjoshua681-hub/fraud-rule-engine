package com.ryanm.fraudruleengine.persistence.repository;

import com.ryanm.fraudruleengine.persistence.entity.TransactionEntity;
import com.ryanm.fraudruleengine.type.TransactionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends CrudRepository<TransactionEntity, UUID> {

    List<TransactionEntity> findByAccountIdAndTimestampAfter(UUID accountId, Instant after);

    List<TransactionEntity> findByAccountIdAndStatusAndTimestampAfter(
            UUID accountId, TransactionStatus status, Instant after);
}
