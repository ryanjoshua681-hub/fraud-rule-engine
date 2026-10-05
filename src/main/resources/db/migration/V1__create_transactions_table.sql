CREATE TABLE IF NOT EXISTS transactions (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id        UUID         NOT NULL,
    amount            NUMERIC(19,4) NOT NULL,
    merchant_code     VARCHAR(100),
    merchant_category VARCHAR(100),
    currency          VARCHAR(3)   NOT NULL DEFAULT 'ZAR',
    timestamp         TIMESTAMPTZ  NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    ip_address        VARCHAR(45),
    device_id         VARCHAR(255),
    country           VARCHAR(3),
    version           BIGINT       NOT NULL DEFAULT 0,
    created_date      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_account_id       ON transactions (account_id);
CREATE INDEX idx_transactions_timestamp        ON transactions (timestamp);
CREATE INDEX idx_transactions_status           ON transactions (status);
CREATE INDEX idx_transactions_account_timestamp ON transactions (account_id, timestamp);
