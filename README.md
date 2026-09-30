# Fraud Rule Engine

A real-time fraud detection service built with **Java 25**, **Spring Boot 4.0.6**, and **Apache Kafka**.

Transactions are evaluated against six configurable rules in parallel using Java 25 Structured Concurrency. Each rule produces a weighted risk score. The aggregate score determines whether a transaction is `ALLOWED`, `FLAGGED`, or `BLOCKED`.

---

## Architecture

```
REST / Kafka → TransactionController / TransactionConsumer
                    ↓
             TransactionService
                    ↓
             FraudRuleEngine  ← StructuredTaskScope (parallel, virtual threads)
          /    |    |    |    |     \
  HighValue  Blacklist  Velocity  Frequency  UnusualHours  RoundAmount
                    ↓
             FraudDecision (sealed RuleResult hierarchy)
                    ↓
             FraudFlagService → PostgreSQL (Spring Data JDBC + Flyway)
                    ↓
             TransactionProducer → Kafka (fraud.flags / transactions.dlq)
```

**Risk scoring:**

| Score   | Level  | Action  |
|---------|--------|---------|
| 0–39    | LOW    | ALLOWED |
| 40–69   | MEDIUM | FLAGGED |
| 70–100  | HIGH   | FLAGGED |
| Any BLOCK result | HIGH | BLOCKED |

---

## Stack

| Technology | Purpose |
|---|---|
| Java 25 (Amazon Corretto) | Virtual threads, structured concurrency, sealed classes |
| Spring Boot 4.0.6 | Application framework |
| Spring Data JDBC | Persistence — explicit SQL, no ORM magic |
| PostgreSQL 16 | Primary data store |
| Apache Kafka (KRaft) | Event streaming |
| Redis 7 | Velocity counters (atomic INCR with TTL) |
| Flyway | Database migrations |
| MapStruct | 3-layer Entity ↔ Domain ↔ DTO mapping |
| Resilience4j | Circuit breaker, rate limiter, retry |
| OpenTelemetry | Distributed tracing |
| Micrometer + Prometheus | Metrics |
| ArchUnit | Architecture rules as tests |

---

## Getting Started

### Prerequisites
- Java 25 (Amazon Corretto)
- Docker Desktop or Rancher Desktop
- Maven 3.9+

### Run locally with Docker Compose

```bash
# Start all infrastructure (PostgreSQL, Redis, Kafka, Kafka UI)
docker compose up -d postgres redis kafka kafka-ui

# Run the app in local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Kafka UI: http://localhost:8090
- Actuator: http://localhost:8080/actuator/health

### Build Docker image (no Docker daemon needed)

```bash
./mvnw package jib:dockerBuild
```

### Run all tests

```bash
./mvnw verify
```

### Integration tests only (Testcontainers — requires Docker)

```bash
./mvnw failsafe:integration-test failsafe:verify
```

### Security scan (OWASP CVE check)

```bash
./mvnw verify -P security
```

### Mutation testing (PITest)

```bash
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage -P mutation
```

---

## API

### POST /api/v1/transactions

Evaluate a transaction:

```json
{
  "accountId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 75000.00,
  "merchantCode": "MERCHANT_001",
  "merchantCategory": "RETAIL",
  "currency": "ZAR",
  "timestamp": "2026-09-30T23:15:00Z",
  "ipAddress": "197.221.10.4",
  "deviceId": "device-abc-123",
  "country": "ZAF"
}
```

Response:

```json
{
  "transactionId": "...",
  "status": "FLAGGED",
  "riskScore": 90,
  "riskLevel": "HIGH",
  "actionTaken": "FLAGGED",
  "triggeredRules": ["HIGH_VALUE_RULE", "UNUSUAL_HOURS_RULE"]
}
```

### GET /api/v1/transactions/{id}

Retrieve a transaction by ID.

### GET /api/v1/fraud-flags/transaction/{transactionId}

Retrieve the fraud evaluation result for a transaction.

---

## Configuration

All rule thresholds are externalised and can be overridden via environment variables or Kubernetes ConfigMap:

| Property | Default | Description |
|---|---|---|
| `app.rules.high-value-threshold` | 50000 | Flag if amount exceeds this (ZAR) |
| `app.rules.velocity-window-seconds` | 60 | Velocity check window |
| `app.rules.velocity-max-transactions` | 5 | Max transactions per window |
| `app.rules.unusual-hours-start` | 23 | Start of suspicious hours (SAST) |
| `app.rules.unusual-hours-end` | 5 | End of suspicious hours (SAST) |
| `app.rules.round-amount-threshold` | 10000 | Minimum for round-amount check |
| `app.rules.blacklisted-merchants` | [] | Comma-separated blocked merchant codes |

---

## Adding a New Rule

1. Create a `@Component` class in `rule/impl/` that implements `FraudRule`
2. Return a `RuleResult.Allow`, `RuleResult.Flag(score, reason)`, or `RuleResult.Block(reason)`
3. The `FraudRuleEngine` auto-discovers it via Spring's `List<FraudRule>` injection — no other changes needed

---

## Kubernetes

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

---

*Ryan Mahabeer — SE2 Application — Capitec Bank 2026*
