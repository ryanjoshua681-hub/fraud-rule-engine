# Project Setup Checklist — Fraud Rule Engine

Track everything that needs to be installed, created, or configured before and during the build.
Update status as each item is completed: ⬜ Not started | 🔄 In progress | ✅ Done

---

## 1. GitHub

| Item | Status | Notes |
|---|---|---|
| Create GitHub account (personal) | ⬜ | Must be a **public** repo — the brief asks for a public GitHub link |
| Create repository: `fraud-rule-engine` | ⬜ | Public repo, no template, add `.gitignore` for Java/Maven |
| Add `README.md` skeleton | ⬜ | Brief requires a README covering build, run, test |
| Add `.gitignore` | ⬜ | Exclude: `target/`, `.env`, `*.class`, IDE files (`.idea/`, `*.iml`) |
| Enable branch protection on `main` | ⬜ | Optional but shows production habits — require PR reviews before merge |

**Why public?** The brief says "public GitHub link". Reviewers need to clone and run it.

---

## 2. Local Development Tools

| Tool | Status | Version | Why you need it | Download |
|---|---|---|---|---|
| **JDK 25** | ⬜ | 25 (LTS) | Compile and run the app. Any distribution works; the Docker image uses Eclipse Temurin 25 | [adoptium.net](https://adoptium.net/) |
| **Maven** | ⬜ | 3.9.x | Build tool — compile, test, package the JAR | [maven.apache.org](https://maven.apache.org/download.cgi) |
| **IntelliJ IDEA** | ⬜ | Latest | IDE — Community edition is free | [jetbrains.com](https://www.jetbrains.com/idea/download/) |
| **Rancher Desktop** | ⬜ | Latest | Local Kubernetes (k3s) — deploy the full stack to a real cluster | [rancherdesktop.io](https://rancherdesktop.io/) |
| **kubectl** | ⬜ | Latest | Kubernetes CLI — deploy manifests to k3s (included with Rancher Desktop) | Comes with Rancher Desktop |

**Rancher Desktop is the only container tool needed.** Set its container engine to **dockerd (moby)** under Preferences → Container Engine so that `docker compose` works.

---

## 4. Kafka (Local — Docker Compose)

Kafka runs in Docker Compose alongside your app. No separate install needed.

| Item | Status | Notes |
|---|---|---|
| Docker Compose file created | ⬜ | Includes: Kafka (KRaft mode), PostgreSQL, Redis, the app itself |
| Topics configured | ⬜ | `transactions.raw`, `fraud.flags`, `transactions.dlq` |

**Why KRaft mode?** Older Kafka versions required ZooKeeper as a separate process — that's extra complexity and an extra container. Kafka 3.3+ supports KRaft mode, which removes the ZooKeeper dependency entirely. We run a single-node Kafka with KRaft: simpler, modern, less memory.

---

## 5. PostgreSQL (Local — Docker Compose)

PostgreSQL also runs in Docker Compose. No separate install needed.

| Item | Status | Notes |
|---|---|---|
| PostgreSQL in Docker Compose | ⬜ | Image: `postgres:16-alpine`. DB: `fraudengine`, user: `fraudengine` |
| Flyway migrations created | ⬜ | V1: transactions table, V2: fraud_flags table, V3: rule_evaluations table |
| pgAdmin or DBeaver (optional) | ⬜ | GUI tool to browse the DB locally. DBeaver is free. |

---

## 6. Kubernetes / Rancher Setup

| Item | Status | Notes |
|---|---|---|
| Rancher Desktop installed | ⬜ | Enable Kubernetes when installing |
| kubectl configured | ⬜ | Run `kubectl get nodes` — should show a single `k3s` node |
| Kubernetes manifests created | ⬜ | `k8s/` folder: Deployment, Service, ConfigMap, Secret, PersistentVolumeClaim |
| App deployed to k3s | ⬜ | `kubectl apply -f k8s/` — app, Kafka, and Postgres running in cluster |

**Note on ArgoCD:** Your team uses ArgoCD for GitOps deployments (it watches a Git repo and auto-deploys changes). That's production-level complexity beyond this submission. We'll demonstrate the k3s deployment manually via `kubectl`. The README will note that ArgoCD would be used in production.

---

## 7. Project Scaffolding

| Item | Status | Notes |
|---|---|---|
| Maven project created | ⬜ | Group: `com.ryanm`, artifact: `fraud-rule-engine`, Java 25 |
| `pom.xml` dependencies added | ⬜ | Spring Boot 3.5.x, Spring Kafka, Spring Data JDBC, PostgreSQL driver, Flyway, Springdoc, Spotless |
| Spotless + Checkstyle configured | ⬜ | Palantir Java Format |
| `application.yml` created | ⬜ | With `local`, `docker` and `k8s` profiles |
| Flyway migration files created | ⬜ | `src/main/resources/db/migration/` |
| Domain model created | ⬜ | `Transaction`, `FraudFlag`, `RuleEvaluation` as Spring Data JDBC aggregates |
| `FraudRule` interface created | ⬜ | Strategy pattern — all rules implement this |
| Rule implementations (5+) | ⬜ | HighValue, HighFrequency, UnusualHours, RoundAmount, Velocity, Blacklist |
| `FraudRuleEngine` created | ⬜ | Orchestrates the rule pipeline |
| Kafka producer + consumer | ⬜ | `TransactionProducer`, `TransactionConsumer` |
| REST controllers | ⬜ | `TransactionController`, `FraudFlagController`, `RuleController` |
| Spring Security (OAuth2) | ⬜ | `SecurityConfig` with JWT resource server |
| Global exception handler | ⬜ | `GlobalExceptionHandler` with RFC 7807 problem detail responses |
| Unit tests (per rule) | ⬜ | One test class per fraud rule |
| Integration test | ⬜ | Testcontainers: Kafka + Postgres — full pipeline test |
| `Dockerfile` | ⬜ | Multi-stage: Maven build stage → Temurin 25 JRE runtime stage |
| `docker-compose.yml` | ⬜ | App + Kafka (KRaft) + PostgreSQL + Redis |
| Kubernetes manifests | ⬜ | `k8s/` folder |
| `README.md` | ⬜ | Build, run, test instructions + architecture overview |

---

## 8. Submission Checklist

Before sending the GitHub link, verify all of these:

| Item | Status |
|---|---|
| Double-click `start.bat` (or `docker compose up --build`) starts the whole stack | ⬜ |
| Swagger UI accessible at `http://localhost:8080/swagger-ui.html` | ⬜ |
| POST a transaction → returns `202 Accepted` | ⬜ |
| GET `/api/v1/fraud/flags` → shows evaluated flags | ⬜ |
| `mvn test` passes with no failures | ⬜ |
| Docker image builds successfully (`docker build .`) | ⬜ |
| App deploys to k3s via `kubectl apply -f k8s/` | ⬜ |
| `README.md` covers: architecture, build, run, test, design decisions | ⬜ |
| No secrets committed to Git (no `.env`, no hardcoded passwords) | ⬜ |
| `DECISIONS.md` reviewed — ready to discuss every choice in the interview | ⬜ |
