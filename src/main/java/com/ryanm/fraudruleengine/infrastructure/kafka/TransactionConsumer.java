package com.ryanm.fraudruleengine.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryanm.fraudruleengine.controller.v1.dto.TransactionRequest;
import com.ryanm.fraudruleengine.service.TransactionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class TransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionConsumer.class);

    private final TransactionService transactionService;
    private final TransactionProducer transactionProducer;
    private final ObjectMapper objectMapper;

    public TransactionConsumer(
            final TransactionService transactionService,
            final TransactionProducer transactionProducer,
            final ObjectMapper objectMapper) {
        this.transactionService = transactionService;
        this.transactionProducer = transactionProducer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.outgoing.topics.raw-transactions}",
            groupId = "fraud-engine-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(
            final ConsumerRecord<String, String> record, final Acknowledgment acknowledgment) {
        try {
            final TransactionRequest request =
                    objectMapper.readValue(record.value(), TransactionRequest.class);
            transactionService.evaluate(request);
            acknowledgment.acknowledge();
            log.info("Processed kafka message key={} partition={} offset={}", record.key(), record.partition(), record.offset());
        } catch (Exception ex) {
            log.error("Failed to process kafka message key={}", record.key(), ex);
            transactionProducer.publishToDlq(record.value(), record.topic(), ex);
            acknowledgment.acknowledge();
        }
    }
}
