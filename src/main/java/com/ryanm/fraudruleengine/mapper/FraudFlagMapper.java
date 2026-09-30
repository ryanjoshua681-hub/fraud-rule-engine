package com.ryanm.fraudruleengine.mapper;

import com.ryanm.fraudruleengine.controller.v1.dto.FraudFlagResponse;
import com.ryanm.fraudruleengine.persistence.entity.FraudFlagEntity;
import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FraudFlagMapper {

    @Mapping(
            target = "triggeredRules",
            expression = "java(splitRules(entity.getTriggeredRules()))")
    FraudFlagResponse toResponse(FraudFlagEntity entity);

    default List<String> splitRules(final String triggeredRules) {
        if (triggeredRules == null || triggeredRules.isBlank()) {
            return List.of();
        }
        return Arrays.asList(triggeredRules.split(","));
    }
}
