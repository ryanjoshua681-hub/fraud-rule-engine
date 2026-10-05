package com.ryanm.fraudruleengine.exception;

import java.util.UUID;

public class FraudFlagNotFoundException extends RuntimeException {

    public FraudFlagNotFoundException(final UUID transactionId) {
        super("Fraud flag not found for transaction: " + transactionId);
    }
}
