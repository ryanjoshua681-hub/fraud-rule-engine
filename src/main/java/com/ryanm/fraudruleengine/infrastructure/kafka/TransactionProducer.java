package com.ryanm.fraudruleengine.infrastructure.kafka;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import com.ryanm.fraudruleengine.rule.FraudDecision;
import com.ryanm.fraudruleengine.type.ActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class TransactionProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationConfigurationProperties config;

    public TransactionProducer(
            final KafkaTemplate<String, Object> kafkaTemplate, final ApplicationConfigurationProperties config) {
        this.kafkaTemplate = kafkaTemplate;
        this.config = config;
    }

    public void publishDecision(final FraudDecision decision) {
        // ALLOWED transactions are persisted to the database — no Kafka notification needed.
        // Publishing them to raw-transactions would re-trigger the consumer in an infinite loop.
        if (decision.action() == ActionType.ALLOWED) {
            return;
        }

        final String topic = config.getKafka().getOutgoing().getTopics().getFraudFlags();

        kafkaTemplate.send(MessageBuilder.withPayload(decision)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, decision.transactionId().toString())
                .setHeader("action", decision.action().name())
                .setHeader("riskLevel", decision.riskLevel().name())
                .build());

        log.info("Published txId={} action={} to topic={}", decision.transactionId(), decision.action(), topic);
    }

    public void publishToDlq(final String payload, final String originalTopic, final Exception cause) {
        final String dlqTopic = config.getKafka().getOutgoing().getTopics().getDlq();
        kafkaTemplate.send(MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, dlqTopic)
                .setHeader("originalTopic", originalTopic)
                .setHeader("errorMessage", cause.getMessage())
                .build());
        log.warn("Sent message to DLQ topic={} originalTopic={}", dlqTopic, originalTopic);
    }
}
