package com.ryanm.fraudruleengine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ryanm.fraudruleengine.type.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model for a financial transaction under fraud evaluation.
 *
 * <p>This is the service-layer representation — not a database entity. It is what {@link
 * com.ryanm.fraudruleengine.rule.FraudRule} implementations receive as input, and what travels on
 * the Kafka {@code transactions.raw} topic. MapStruct mappers convert between this and the
 * persistence entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {

    private UUID id;

    private UUID accountId;

    /** Transaction amount in ZAR (South African Rand). */
    private BigDecimal amount;

    private String merchantCode;

    private String merchantCategory;

    private String currency;

    /** UTC timestamp of when the transaction was initiated. */
    private Instant timestamp;

    private TransactionStatus status;

    private String ipAddress;

    private String deviceId;

    /** ISO 3166-1 alpha-2 country code of the transaction origin. */
    private String country;
}
