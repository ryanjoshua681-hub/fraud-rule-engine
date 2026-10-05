package com.ryanm.fraudruleengine.persistence.entity;

import com.ryanm.fraudruleengine.type.ActionType;
import com.ryanm.fraudruleengine.type.RiskLevel;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("fraud_flags")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FraudFlagEntity {

    @Id
    @Column("id")
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column("transaction_id")
    private UUID transactionId;

    @Column("risk_score")
    private int riskScore;

    @Column("risk_level")
    private RiskLevel riskLevel;

    @Column("action_taken")
    private ActionType actionTaken;

    /** Comma-separated list of rule names that contributed to this flag. */
    @Column("triggered_rules")
    private String triggeredRules;

    @Version
    @Column("version")
    private Long version;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;
}
