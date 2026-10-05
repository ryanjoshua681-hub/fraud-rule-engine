package com.ryanm.fraudruleengine.persistence.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("rule_evaluations")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RuleEvaluationEntity {

    @Id
    @Column("id")
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column("fraud_flag_id")
    private UUID fraudFlagId;

    @Column("rule_name")
    private String ruleName;

    /** ALLOW, FLAG, or BLOCK — mirrors the RuleResult sealed type. */
    @Column("result_type")
    private String resultType;

    @Column("score")
    private int score;

    @Column("reason")
    private String reason;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;
}
