CREATE TABLE IF NOT EXISTS fraud_flags (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id   UUID        NOT NULL REFERENCES transactions (id),
    risk_score       INT         NOT NULL,
    risk_level       VARCHAR(20) NOT NULL,
    action_taken     VARCHAR(20) NOT NULL,
    triggered_rules  TEXT,
    version          BIGINT      NOT NULL DEFAULT 0,
    created_date     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_fraud_flags_transaction_id ON fraud_flags (transaction_id);
CREATE        INDEX idx_fraud_flags_risk_level     ON fraud_flags (risk_level);
CREATE        INDEX idx_fraud_flags_action_taken   ON fraud_flags (action_taken);
