# Architecture Decision Log — Fraud Rule Engine

A running record of technology choices made during this project, the alternatives considered, and the reasoning. Use this to prepare for the interview — you will be asked *why* you made these decisions.

---

## 1. Kafka vs RabbitMQ for Transaction Event Streaming

**Decision:** Apache Kafka

**Alternatives considered:** RabbitMQ, a plain database table used as a queue

### Why Kafka?
| Factor | Kafka | RabbitMQ |
|---|---|---|
| Event replay | Yes — reprocess historical transactions if rules change | No — messages deleted once acknowledged |
| Multiple independent consumers | Yes — each consumer group reads the whole topic | Needs a queue per consumer (fan-out exchange) |
| Throughput at scale | Millions/sec across partitions | High, but scales less naturally |
| Industry relevance (banking) | Standard at most major banks | Common, mostly for task queues |
| Ops complexity | Higher — broker plus partition/offset concepts | Lower — simpler mental model |

### The honest challenge
For *this single project* (one producer, one consumer), RabbitMQ is simpler and sufficient. Kafka is architecturally heavier than the problem strictly requires.

### Why Kafka is still the right call for this submission
The submission is evaluated as production-grade work, not a toy project. Choosing Kafka signals:
- You understand how real banking transaction pipelines are built
- You've thought about replay, scale, and multi-consumer architectures
- You chose the harder path intentionally, not because it was easy

### What to say if challenged in the interview
> "For this project's scale, RabbitMQ would work fine. I chose Kafka because it reflects what production banking systems actually use, and because the event replay capability is genuinely valuable for fraud — if we improve a rule, we can reprocess historical transactions without re-ingesting them."

---

## 2. Java 21 vs Java 17 (or older)

**Decision:** Java 21

**Alternatives considered:** Java 17, Java 11

### Why Java 21?
- Java 21 is the current **LTS (Long Term Support)** release — the safe, stable choice for new production systems
- Ships with **virtual threads** (Project Loom): the JVM handles thousands of concurrent I/O operations without needing a large thread pool. For a fraud engine that's doing DB reads + Kafka polls simultaneously, this matters
- Modern language features: records, pattern matching, sealed classes — write less boilerplate
- Spring Boot 3.x supports Java 17+ but is optimised for 21

### Why not Java 17?
17 is still supported but 21 is the better LTS to target for anything starting in 2024+. No downside to using 21 on a new project.

### Why not Java 11?
Java 11 is end-of-life for most vendors. Starting a new project on it would be a red flag in a code review.

---

## 3. PostgreSQL vs a NoSQL key-value store

**Decision:** PostgreSQL (a container in Docker Compose / Kubernetes)

**Alternatives considered:** a NoSQL key-value store (e.g. MongoDB, Cassandra), H2 (in-memory)

### Why PostgreSQL?
Fraud data is inherently **relational**:
- A `FraudFlag` belongs to a `Transaction`, which belongs to an `Account`
- You need to query: *"All flags for account X"*, *"Which rules fired most in the last 7 days?"*, *"Audit trail for flag Z"*
- These are join-heavy, filter-heavy queries — exactly what a relational database is built for

### Why not a key-value store?
A key-value store optimised for single-item lookups by a known key at massive scale (think: Twitter looking up one user's timeline). The query patterns of a fraud engine don't fit that model well. You'd end up writing inefficient scan operations or over-engineering your key design.

A key-value store is the right tool when: you know your access patterns upfront, they're simple, and you need global scale. Fraud analytics is the wrong use case.

### Why not H2?
H2 is an in-memory database — data disappears when the app restarts. Fine for unit tests (and we use it there), but not for a production-grade submission. The evaluators expect to see persistence that survives a restart.

---

## 4. Spring Boot vs Quarkus vs Micronaut

**Decision:** Spring Boot 3.3.x

**Alternatives considered:** Quarkus, Micronaut, plain Spring

### Why Spring Boot?
- Industry standard for Java backend services — especially in enterprise and banking
- Widely used across enterprise and banking Java teams, so most reviewers will already know it
- Huge ecosystem: Spring Kafka, Spring Security, Spring Data JPA, Springdoc — everything we need has a first-class integration
- Spring Boot 3.x with virtual threads (Java 21) closes most of the startup-time gap with Quarkus

### Why not Quarkus?
Quarkus has faster startup and lower memory footprint — genuinely better for Lambda/serverless. But for a long-running service (which a fraud engine is), the difference is small. More importantly, it's less familiar to most banking-environment reviewers, so the benefit doesn't outweigh the familiarity cost.

### What to say if challenged
> "I chose Spring Boot because it's the dominant framework in enterprise Java and gives me first-class integrations with everything I need. If this were a serverless or GraalVM native use case, Quarkus would be worth evaluating — but for a long-running event-processing service, Spring Boot is the right fit."

---

## 5. Rancher Desktop (k3s) vs Docker Compose only

**Decision:** Both — Docker Compose for local running (required by brief) + Kubernetes manifests for Rancher Desktop

**Alternatives considered:** Docker Compose only

### Why include Kubernetes?
The brief only asks for Docker. Including Kubernetes manifests is **above and beyond** — it demonstrates:
- You understand the gap between "containerised" and "production-deployed"
- You know how secrets, config, and persistent storage are managed in real deployments
- You've thought about scaling (replicas, resource limits)

### Why Rancher Desktop specifically?
- Free, runs on Windows (which is your OS)
- Ships with k3s — a lightweight but fully conformant Kubernetes distribution
- Rancher is used by enterprises (including banks) to manage production Kubernetes clusters, so the tool is directly relevant

### Why not Docker Compose alone?
Compose is the right tool for running the stack on one machine, and it is the one-command path in this project. Kubernetes is cloud-agnostic — the same manifests run on any managed cluster (Azure AKS, GCP GKE, and others) or on Rancher Desktop locally — so the manifests show the path to production without tying the project to one cloud.

---

## 6. Flyway vs Liquibase for DB Migrations

**Decision:** Flyway

**Alternatives considered:** Liquibase, manual SQL scripts

### Why Flyway?
Database migrations track schema changes over time — like git for your database. Without them, you can't reliably reproduce the schema in a new environment.

Flyway uses plain SQL files (`V1__create_transactions.sql`) — simple to read, simple to write, no XML/YAML DSL to learn. Spring Boot auto-configures Flyway with zero extra setup.

### Why not Liquibase?
Liquibase is more powerful (supports XML, YAML, JSON, SQL formats and rollback operations) but that power adds complexity. For this project, Flyway's simplicity wins.

### Why not manual SQL scripts?
Running raw SQL manually in a new environment is error-prone and not reproducible. Flyway makes the schema part of the codebase — it runs automatically on startup and every environment gets the same schema.

---

## 7. Spring Data JDBC vs Spring Data JPA (Hibernate)

**Decision:** Spring Data JDBC

**Alternatives considered:** Spring Data JPA (Hibernate), plain JDBC (JdbcTemplate)

### The difference
Both let you map Java objects to database tables. The key difference is the layer of magic involved:

**Spring Data JPA (Hibernate):**
- An ORM (Object-Relational Mapper) — Hibernate tracks every object loaded from the DB in a "session"
- Supports lazy loading: fetch child records only when accessed
- Supports cascading: save a parent and children save automatically
- Rich query DSL via JPQL, Criteria API, or derived methods

**Spring Data JDBC:**
- No session, no proxy objects, no lazy loading — what you load is what you get
- You are in control of every query: if you want related data, you fetch it explicitly
- Designed for DDD aggregate roots — one repository per aggregate, no cross-aggregate navigation
- Simpler, faster, less surprising

### Why JDBC for this project?
Spring Data JDBC is a deliberate architectural choice here, not a default. It enforces clear boundaries between aggregates and prevents the most common Hibernate pitfalls: `LazyInitializationException`, N+1 query problems, unintended cascades.

For a fraud engine, this is a good fit: a `FraudFlag` is an aggregate root. When you load it, you want its `RuleEvaluations`. With JDBC you fetch them explicitly — no surprises.

### What to say if challenged
> "I chose Spring Data JDBC over JPA because it forces explicit, predictable data access. There's no Hibernate session magic — every query is intentional. This makes the code easier to reason about and profile, which matters in a high-throughput system like a fraud engine."

### Why not plain JdbcTemplate?
JdbcTemplate gives you full SQL control but no mapping — you write `ResultSet` row mappers manually. Spring Data JDBC gives you the same SQL predictability with repository conventions and automatic entity mapping. It's the right middle ground.

---

## 8. Java 25 vs Java 21

**Decision:** Java 25

**Alternatives considered:** Java 21 (previous LTS), Java 17

### Why Java 25?
Java releases a new LTS version every 4 years. The LTS versions are: 11 → 17 → 21 → **25** (released September 2025). Java 25 is the current LTS as of this project.

Java 25 is the current LTS release — using anything older for a new project would be moving backwards.

Java 25 improvements over 21 relevant to this project:
- Continued **virtual thread** improvements (Project Loom) — high-concurrency I/O without thread-per-request overhead
- Stable **Structured Concurrency** API — cleaner handling of parallel subtasks (useful for running multiple fraud rules concurrently)
- **Value types** (Project Valhalla) — more memory-efficient domain objects

### Why not Java 21?
Java 21 is still supported but is the *previous* LTS. Starting a new project on 21 in late 2025/2026 is like starting on Java 17 when 21 was available — technically fine, but not forward-looking.

---

## 9. Spring Security OAuth2 Resource Server vs Custom JWT Filter

**Decision:** Spring Security OAuth2 Resource Server (`spring-boot-starter-oauth2-resource-server`)

**Alternatives considered:** Custom `OncePerRequestFilter` JWT filter, Spring Security basic auth

### Why OAuth2 Resource Server?
A custom JWT filter means you write the token parsing, signature verification, claims extraction, and error handling yourself. That's a lot of security-critical code where a bug means bypassed authentication.

`spring-boot-starter-oauth2-resource-server` does all of this for you:
- Validates JWT signature against a JWKS endpoint (or a local public key for dev)
- Extracts authorities/roles from claims automatically
- Returns proper `401 Unauthorized` / `403 Forbidden` responses

This is the standard, production-grade approach for securing services.

### For the submission (no Keycloak/IdP available)
In dev/test mode, configure the resource server with a symmetric key (HS256) so you can generate test tokens without a running identity provider. Document in the README how to get a token and how to configure a real IdP in production.

### What to say if challenged
> "I used Spring's OAuth2 Resource Server rather than a custom filter because security code shouldn't be reinvented — the starter handles JWT validation, signature checking, and error responses correctly. In production this would point to a Keycloak or similar JWKS endpoint; for the submission I've configured it with a symmetric key so the reviewer can generate test tokens without extra infrastructure."

---

## 10. Spotless + Checkstyle for Code Quality

**Decision:** Spotless (Palantir Java Format) + Checkstyle Maven plugins

**Alternatives considered:** Manual formatting, IntelliJ formatter only

### Why automated formatting?
Code formatting debates waste time in code reviews. Enforcing a formatter at build time means the diff shows only meaningful changes, not whitespace noise.

**Palantir Java Format** is enforced via the **Spotless Maven plugin** — it runs during the build and fails if the code isn't formatted correctly. This is standard practice on production teams.

Including it in your submission signals you understand production engineering standards, not just "code that compiles."

### Why Palantir Java Format over Google Java Format?
Palantir's formatter produces slightly more readable output for longer method chains and lambdas. The choice matters less than the fact that *a* formatter is enforced.

---

*This document will be updated as more decisions are made throughout the project.*
