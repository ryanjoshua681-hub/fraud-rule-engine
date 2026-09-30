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
| **JDK 25** | ⬜ | 25 (LTS) | Compile and run the app. Use Amazon Corretto 25 — same base image as Capitec | [corretto.aws](https://aws.amazon.com/corretto/) |
| **Maven** | ⬜ | 3.9.x | Build tool — compile, test, package the JAR | [maven.apache.org](https://maven.apache.org/download.cgi) |
| **IntelliJ IDEA** | ⬜ | Latest | IDE — Community edition is free | [jetbrains.com](https://www.jetbrains.com/idea/download/) |
| **Docker Desktop** | ⬜ | Latest | Runs Kafka + PostgreSQL locally in containers | [docker.com](https://www.docker.com/products/docker-desktop/) |
| **Rancher Desktop** | ⬜ | Latest | Local Kubernetes (k3s) — deploy the full stack to a real cluster | [rancherdesktop.io](https://rancherdesktop.io/) |
| **AWS CLI v2** | ⬜ | v2 | Push images to ECR, manage SSM parameters, interact with RDS | [aws CLI install](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) |
| **kubectl** | ⬜ | Latest | Kubernetes CLI — deploy manifests to k3s (included with Rancher Desktop) | Comes with Rancher Desktop |

**Note on Rancher Desktop vs Docker Desktop:** You only need one for local container running. Rancher Desktop includes both a container runtime AND Kubernetes. If you install Rancher Desktop, you may not need Docker Desktop separately. Install Rancher Desktop first and see if Docker Compose works via it.

---

## 3. AWS Account & Services

### Account Setup

| Item | Status | Notes |
|---|---|---|
| Create AWS account | ⬜ | New account = full 12-month free tier. Use a personal email. |
| Enable MFA on root account | ⬜ | Security best practice — you'll know why working at a bank |
| Create IAM user for local dev | ⬜ | Never use root credentials. Create `fraud-engine-dev` IAM user with programmatic access |
| Configure AWS CLI profile | ⬜ | Run `aws configure` with the IAM user's access key + secret. Region: `af-south-1` (same as Capitec) or `eu-west-1` |

### Free-Tier Services to Set Up

| Service | Status | What to create | Free tier limit |
|---|---|---|---|
| **ECR** (Elastic Container Registry) | ⬜ | Create repository: `fraud-rule-engine` | 500 MB/month — always free |
| **SSM Parameter Store** | ⬜ | Create parameters: `/fraud-engine/db/password`, `/fraud-engine/jwt/secret` | Free for Standard parameters |
| **CloudWatch** | ⬜ | Log group: `/fraud-rule-engine` — auto-created when app starts | 5 GB logs + 10 metrics/month free |
| **RDS PostgreSQL** | ⬜ | Instance: `fraud-engine-db`, engine: PostgreSQL 16, class: `db.t3.micro` | 750 hrs/month for 12 months |

**Warning on RDS:** The free tier clock starts when you create the instance. Don't create it until you're ready to use it. For local development, PostgreSQL runs in Docker — you only need RDS when you want to demo a cloud deployment.

---

## 4. Kafka (Local — Docker Compose)

Kafka runs in Docker Compose alongside your app. No separate install needed.

| Item | Status | Notes |
|---|---|---|
| Docker Compose file created | ⬜ | Will include: Kafka (KRaft mode), PostgreSQL, Kafka UI, the app itself |
| Topics configured | ⬜ | `transactions.raw`, `fraud.flags`, `transactions.dlq` |
| Kafka UI accessible | ⬜ | `http://localhost:9000` — lets you see messages on topics visually |

**Why KRaft mode?** Older Kafka versions required ZooKeeper as a separate process — that's extra complexity and an extra container. Kafka 3.3+ supports KRaft mode, which removes the ZooKeeper dependency entirely. We run a single-node Kafka with KRaft: simpler, modern, less memory.

**Production equivalent:** AWS MSK with IAM authentication. The `application.yml` will have a `cloud` profile that switches from Docker Kafka to MSK — same code, different config.

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
| `pom.xml` dependencies added | ⬜ | Spring Boot 3.5.x, Spring Kafka, Spring Data JDBC, PostgreSQL driver, Flyway, Springdoc, Spotless, AWS SDK v2 |
| Spotless + Checkstyle configured | ⬜ | Palantir Java Format — matches Capitec team standard |
| `application.yml` created | ⬜ | With `local` and `cloud` profiles |
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
| `Dockerfile` | ⬜ | Multi-stage: Maven build stage → Corretto 25 runtime stage |
| `docker-compose.yml` | ⬜ | App + Kafka (KRaft) + PostgreSQL + Kafka UI |
| Kubernetes manifests | ⬜ | `k8s/` folder |
| `README.md` | ⬜ | Build, run, test instructions + architecture overview |

---

## 8. Submission Checklist

Before sending the GitHub link, verify all of these:

| Item | Status |
|---|---|
| `docker compose up` starts without errors | ⬜ |
| Swagger UI accessible at `http://localhost:8080/swagger-ui.html` | ⬜ |
| Can get a JWT token via `/api/v1/auth/token` | ⬜ |
| POST a transaction → returns `202 Accepted` | ⬜ |
| GET `/api/v1/fraud/flags` → shows evaluated flags | ⬜ |
| Kafka UI shows messages on `transactions.raw` topic | ⬜ |
| `mvn test` passes with no failures | ⬜ |
| Docker image builds successfully (`docker build .`) | ⬜ |
| Docker image pushed to ECR | ⬜ |
| App deploys to k3s via `kubectl apply -f k8s/` | ⬜ |
| `README.md` covers: architecture, build, run, test, design decisions | ⬜ |
| No secrets committed to Git (no `.env`, no hardcoded passwords) | ⬜ |
| `DECISIONS.md` reviewed — ready to discuss every choice in the interview | ⬜ |
