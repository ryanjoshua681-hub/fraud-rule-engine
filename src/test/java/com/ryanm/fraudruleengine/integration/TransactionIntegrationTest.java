package com.ryanm.fraudruleengine.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryanm.fraudruleengine.controller.v1.dto.FraudFlagResponse;
import com.ryanm.fraudruleengine.controller.v1.dto.TransactionRequest;
import com.ryanm.fraudruleengine.controller.v1.dto.TransactionResponse;
import com.ryanm.fraudruleengine.type.ActionType;
import com.ryanm.fraudruleengine.type.RiskLevel;
import com.ryanm.fraudruleengine.type.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end integration tests using real PostgreSQL, Redis, and Kafka containers.
 *
 * <p>Each test submits a transaction via the HTTP API and verifies the fraud decision persisted
 * to PostgreSQL, the fraud flag recorded by FraudFlagService, and the HTTP response shape.
 * Redis backs VelocityRule; Kafka receives the published decision after each evaluation.
 *
 * <p>Profiles: "test" activates the permit-all security filter chain; "integration-test" supplies
 * concrete property values so Spring does not try to resolve ${spring.embedded.kafka.brokers}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "integration-test"})
@Testcontainers
class TransactionIntegrationTest {

    // ── Infrastructure containers ─────────────────────────────────────────────

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * KafkaConfig reads ${spring.kafka.bootstrap-servers} via @Value, not KafkaConnectionDetails,
     * so @ServiceConnection alone is insufficient — @DynamicPropertySource writes the address
     * directly into the Environment before the application context starts.
     */
    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    @ServiceConnection
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void kafkaBootstrapServers(final DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    // ── Test dependencies ─────────────────────────────────────────────────────

    @Autowired
    private TestRestTemplate restTemplate;

    // ── Safe daytime timestamp: 10:00 SAST = 08:00 UTC, well outside the 23–05 unusual-hours window ──

    private static final Instant DAYTIME_SAST = Instant.parse("2026-10-01T08:00:00Z");

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void givenNormalTransaction_whenEvaluate_thenReturnAllowedWithZeroRiskScore() {
        final TransactionRequest request =
                buildRequest(UUID.randomUUID(), BigDecimal.valueOf(1_000), "NORMAL_MERCHANT", DAYTIME_SAST);

        final ResponseEntity<TransactionResponse> response =
                restTemplate.postForEntity("/api/v1/transactions", request, TransactionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final TransactionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.transactionId()).isNotNull();
        assertThat(body.status()).isEqualTo(TransactionStatus.ALLOWED);
        assertThat(body.actionTaken()).isEqualTo(ActionType.ALLOWED);
        assertThat(body.riskScore()).isEqualTo(0);
        assertThat(body.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(body.triggeredRules()).isEmpty();
    }

    @Test
    void givenNormalTransaction_whenEvaluate_thenLocationHeaderPointsToCreatedResource() {
        final TransactionRequest request =
                buildRequest(UUID.randomUUID(), BigDecimal.valueOf(500), "NORMAL_MERCHANT", DAYTIME_SAST);

        final ResponseEntity<TransactionResponse> response =
                restTemplate.postForEntity("/api/v1/transactions", request, TransactionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final UUID transactionId = response.getBody().transactionId();
        assertThat(response.getHeaders().getLocation()).hasPath("/api/v1/transactions/" + transactionId);
    }

    @Test
    void givenHighValueTransaction_whenEvaluate_thenReturnFlaggedWithMediumRisk() {
        // 75,500 ZAR: above the 50,000 high-value threshold but not round (% 1000 != 0)
        // so only HIGH_VALUE_RULE fires — score 60 → MEDIUM risk
        final TransactionRequest request =
                buildRequest(UUID.randomUUID(), BigDecimal.valueOf(75_500), "NORMAL_MERCHANT", DAYTIME_SAST);

        final ResponseEntity<TransactionResponse> response =
                restTemplate.postForEntity("/api/v1/transactions", request, TransactionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final TransactionResponse body = response.getBody();
        assertThat(body.status()).isEqualTo(TransactionStatus.FLAGGED);
        assertThat(body.actionTaken()).isEqualTo(ActionType.FLAGGED);
        assertThat(body.riskScore()).isEqualTo(60);
        assertThat(body.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(body.triggeredRules()).containsExactly("HIGH_VALUE_RULE");
    }

    @Test
    void givenHighValueAndRoundAmount_whenEvaluate_thenReturnFlaggedWithHighRisk() {
        // 100,000 ZAR: above 50,000 threshold AND divisible by 1,000 AND above 10,000 threshold
        // HIGH_VALUE_RULE (60) + ROUND_AMOUNT_RULE (25) = 85 → HIGH risk
        final TransactionRequest request =
                buildRequest(UUID.randomUUID(), BigDecimal.valueOf(100_000), "NORMAL_MERCHANT", DAYTIME_SAST);

        final ResponseEntity<TransactionResponse> response =
                restTemplate.postForEntity("/api/v1/transactions", request, TransactionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final TransactionResponse body = response.getBody();
        assertThat(body.status()).isEqualTo(TransactionStatus.FLAGGED);
        assertThat(body.riskScore()).isEqualTo(85);
        assertThat(body.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(body.triggeredRules()).containsExactlyInAnyOrder("HIGH_VALUE_RULE", "ROUND_AMOUNT_RULE");
    }

    @Test
    void givenBlacklistedMerchant_whenEvaluate_thenReturnBlocked() {
        final TransactionRequest request =
                buildRequest(UUID.randomUUID(), BigDecimal.valueOf(500), "BLOCKED_MERCHANT_001", DAYTIME_SAST);

        final ResponseEntity<TransactionResponse> response =
                restTemplate.postForEntity("/api/v1/transactions", request, TransactionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final TransactionResponse body = response.getBody();
        assertThat(body.status()).isEqualTo(TransactionStatus.BLOCKED);
        assertThat(body.actionTaken()).isEqualTo(ActionType.BLOCKED);
        assertThat(body.triggeredRules()).contains("BLACKLIST_RULE");
    }

    @Test
    void givenEvaluatedTransaction_whenGetById_thenReturnSameTransaction() {
        final UUID accountId = UUID.randomUUID();
        final TransactionRequest request =
                buildRequest(accountId, BigDecimal.valueOf(2_500), "NORMAL_MERCHANT", DAYTIME_SAST);

        final TransactionResponse created = restTemplate
                .postForEntity("/api/v1/transactions", request, TransactionResponse.class)
                .getBody();
        assertThat(created).isNotNull();

        final ResponseEntity<TransactionResponse> fetched =
                restTemplate.getForEntity("/api/v1/transactions/" + created.transactionId(), TransactionResponse.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        final TransactionResponse body = fetched.getBody();
        assertThat(body.transactionId()).isEqualTo(created.transactionId());
        assertThat(body.accountId()).isEqualTo(accountId);
        assertThat(body.amount()).isEqualByComparingTo(BigDecimal.valueOf(2_500));
        assertThat(body.status()).isEqualTo(TransactionStatus.ALLOWED);
    }

    @Test
    void givenUnknownTransactionId_whenGetById_thenReturn404() {
        final ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/transactions/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void givenFlaggedTransaction_whenGetFraudFlag_thenReturnFlagWithRuleDetails() {
        // Evaluate a high-value transaction so a fraud flag is created
        final UUID accountId = UUID.randomUUID();
        final TransactionRequest request =
                buildRequest(accountId, BigDecimal.valueOf(75_500), "NORMAL_MERCHANT", DAYTIME_SAST);

        final TransactionResponse evaluated = restTemplate
                .postForEntity("/api/v1/transactions", request, TransactionResponse.class)
                .getBody();
        assertThat(evaluated).isNotNull();

        final ResponseEntity<FraudFlagResponse> flagResponse = restTemplate.getForEntity(
                "/api/v1/fraud-flags/transaction/" + evaluated.transactionId(), FraudFlagResponse.class);

        assertThat(flagResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        final FraudFlagResponse flag = flagResponse.getBody();
        assertThat(flag).isNotNull();
        assertThat(flag.transactionId()).isEqualTo(evaluated.transactionId());
        assertThat(flag.riskScore()).isEqualTo(60);
        assertThat(flag.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(flag.actionTaken()).isEqualTo(ActionType.FLAGGED);
        assertThat(flag.triggeredRules()).containsExactly("HIGH_VALUE_RULE");
        assertThat(flag.createdDate()).isNotNull();
    }

    @Test
    void givenRequestWithNullAccountId_whenEvaluate_thenReturn400() {
        // accountId is @NotNull in TransactionRequest — missing it must return 400
        final TransactionRequest invalidRequest = new TransactionRequest(
                null, BigDecimal.valueOf(1_000), "NORMAL_MERCHANT", null, "ZAR", DAYTIME_SAST, null, null, null);

        final ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/transactions", invalidRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void givenTransactionForUnknownFraudFlag_whenGetFraudFlag_thenReturn404() {
        final ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/fraud-flags/transaction/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static TransactionRequest buildRequest(
            final UUID accountId, final BigDecimal amount, final String merchantCode, final Instant timestamp) {
        return new TransactionRequest(accountId, amount, merchantCode, null, "ZAR", timestamp, null, null, null);
    }
}
