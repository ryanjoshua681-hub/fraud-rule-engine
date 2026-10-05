package com.ryanm.fraudruleengine.controller.v1.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionRequest(
        @NotNull UUID accountId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String merchantCode,
        String merchantCategory,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull Instant timestamp,
        String ipAddress,
        String deviceId,
        String country) {}
