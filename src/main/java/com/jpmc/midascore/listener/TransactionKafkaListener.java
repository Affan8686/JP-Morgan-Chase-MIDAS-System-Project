package com.jpmc.midascore.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpmc.midascore.service.TransactionService;
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
    private final TransactionService transactionService;
    private final List<com.jpmc.midascore.foundation.Transaction> receivedTransactions;

    public TransactionKafkaListener(ObjectMapper objectMapper,
                                    TransactionService transactionService) {
        this.objectMapper = objectMapper;
        this.transactionService = transactionService;
        this.receivedTransactions = new ArrayList<>();
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message) {
        try {
            logger.debug("Received message from Kafka: {}", message);

            // Deserialize JSON message to foundation.Transaction object
            com.jpmc.midascore.foundation.Transaction transaction =
                    objectMapper.readValue(message, com.jpmc.midascore.foundation.Transaction.class);

            // Store the transaction (for testing purposes)
            receivedTransactions.add(transaction);

            // Process the transaction using the service
            transactionService.processTransaction(transaction);

            logger.info("Successfully processed transaction: Sender={}, Recipient={}, Amount={}",
                    transaction.getSenderId(),
                    transaction.getRecipientId(),
                    transaction.getAmount());

        } catch (Exception e) {
            logger.error("Error processing Kafka message: {}", message, e);
        }
    }

    public List<com.jpmc.midascore.foundation.Transaction> getReceivedTransactions() {
        return new ArrayList<>(receivedTransactions);
    }

    public void clearReceivedTransactions() {
        receivedTransactions.clear();
    }
}
//```
//
//        ## Key Changes Made:
//
//        1. **foundation/Transaction.java** - Changed from `BigDecimal` to `float` to match what `KafkaProducer` sends
//2. **TransactionKafkaListener.java** - Simplified to directly deserialize to `foundation.Transaction` instead of `model.Transaction`
//
//        ## Summary of What We're Doing:
//
//        - `KafkaProducer` sends `foundation.Transaction` as JSON with `float` amount
//- `TransactionKafkaListener` receives and deserializes to `foundation.Transaction`
//        - `TransactionService` processes the `foundation.Transaction`
//        - `TransactionRecord` entity stores the processed transaction with sender/recipient relationships
//
//The data flow is:
//        ```
//Kafka (JSON) → foundation.Transaction → TransactionService → TransactionRecord (entity in DB)