package com.ryanm.fraudruleengine.persistence.entity;

import com.ryanm.fraudruleengine.type.TransactionStatus;
import java.math.BigDecimal;
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

@Table("transactions")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    @Id
    @Column("id")
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column("account_id")
    private UUID accountId;

    @Column("amount")
    private BigDecimal amount;

    @Column("merchant_code")
    private String merchantCode;

    @Column("merchant_category")
    private String merchantCategory;

    @Column("currency")
    private String currency;

    @Column("timestamp")
    private Instant timestamp;

    @Column("status")
    private TransactionStatus status;

    @Column("ip_address")
    private String ipAddress;

    @Column("device_id")
    private String deviceId;

    @Column("country")
    private String country;

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
