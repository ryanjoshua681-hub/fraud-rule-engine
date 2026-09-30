CREATE TABLE IF NOT EXISTS rule_evaluations (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    fraud_flag_id UUID        NOT NULL REFERENCES fraud_flags (id),
    rule_name    VARCHAR(100) NOT NULL,
    result_type  VARCHAR(10)  NOT NULL,
    score        INT          NOT NULL DEFAULT 0,
    reason       TEXT,
    created_date TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rule_evaluations_fraud_flag_id ON rule_evaluations (fraud_flag_id);
CREATE INDEX idx_rule_evaluations_rule_name     ON rule_evaluations (rule_name);
