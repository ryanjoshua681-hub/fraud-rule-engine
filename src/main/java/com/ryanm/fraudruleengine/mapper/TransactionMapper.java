package com.ryanm.fraudruleengine.mapper;

import com.ryanm.fraudruleengine.controller.v1.dto.TransactionRequest;
import com.ryanm.fraudruleengine.model.Transaction;
import com.ryanm.fraudruleengine.persistence.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    Transaction toDomain(TransactionRequest request);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    TransactionEntity toEntity(Transaction domain);

    Transaction toDomain(TransactionEntity entity);
}
