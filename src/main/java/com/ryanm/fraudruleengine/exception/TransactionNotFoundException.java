package com.ryanm.fraudruleengine.exception;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(final UUID id) {
        super("Transaction not found: " + id);
    }
}
