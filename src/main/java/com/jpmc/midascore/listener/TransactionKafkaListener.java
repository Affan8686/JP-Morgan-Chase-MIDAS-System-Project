package com.jpmc.midascore.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpmc.midascore.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TransactionKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final List<Transaction> receivedTransactions;

    public TransactionKafkaListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.receivedTransactions = new ArrayList<>();
    }

    /**
     * Listens to the Kafka topic defined in application.properties
     * Deserializes incoming JSON messages to Transaction objects
     */
    @KafkaListener(topics = "${general.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message) {
        try {
            logger.debug("Received message from Kafka: {}", message);

            // Deserialize JSON message to Transaction object
            Transaction transaction = objectMapper.readValue(message, Transaction.class);

            // Store the transaction (for testing purposes)
            receivedTransactions.add(transaction);

            logger.info("Successfully processed transaction: ID={}, Amount={}, Account={}",
                    transaction.getId(),
                    transaction.getAmount(),
                    transaction.getAccountId());

            // TODO: In future tasks, pass transaction to service layer for processing

        } catch (Exception e) {
            logger.error("Error processing Kafka message: {}", message, e);
            // TODO: Implement error handling strategy (dead letter queue, retry, etc.)
        }
    }

    /**
     * Returns all transactions received by this listener
     * Useful for testing and debugging
     */
    public List<Transaction> getReceivedTransactions() {
        return new ArrayList<>(receivedTransactions);
    }

    /**
     * Clears the list of received transactions
     * Useful for testing
     */
    public void clearReceivedTransactions() {
        receivedTransactions.clear();
    }
}