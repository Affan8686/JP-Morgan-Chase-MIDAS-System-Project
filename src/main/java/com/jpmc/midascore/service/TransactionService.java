package com.jpmc.midascore.service;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private static final String INCENTIVE_API_URL = "http://localhost:8080/incentive";

    private final UserRepository userRepository;
    private final TransactionRecordRepository recordRepository;
    private final RestTemplate restTemplate;

    public TransactionService(UserRepository userRepository,
                              TransactionRecordRepository recordRepository) {
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public void processTransaction(Transaction tx) {
        try {
            logger.debug("Processing transaction: sender={}, recipient={}, amount={}",
                    tx.getSenderId(), tx.getRecipientId(), tx.getAmount());

            // Fetch sender and recipient
            UserRecord sender = userRepository.findById(tx.getSenderId());
            UserRecord recipient = userRepository.findById(tx.getRecipientId());

            // Validation: Check if sender exists
            if (sender == null) {
                logger.warn("Invalid sender ID: {}", tx.getSenderId());
                return; // Discard transaction
            }

            // Validation: Check if recipient exists
            if (recipient == null) {
                logger.warn("Invalid recipient ID: {}", tx.getRecipientId());
                return; // Discard transaction
            }

            // Validation: Check if sender has sufficient balance
            if (sender.getBalance() < tx.getAmount()) {
                logger.warn("Insufficient balance for sender {}: has {}, needs {}",
                        sender.getName(), sender.getBalance(), tx.getAmount());
                return; // Discard transaction
            }

            // All validations passed - call incentive API
            float incentiveAmount = 0.0f;
            try {
                logger.info("Calling incentive API with transaction: senderId={}, recipientId={}, amount={}",
                        tx.getSenderId(), tx.getRecipientId(), tx.getAmount());

                Incentive incentive = restTemplate.postForObject(
                        INCENTIVE_API_URL,
                        tx,
                        Incentive.class
                );

                if (incentive != null) {
                    incentiveAmount = (float) incentive.getAmount();
                    logger.info("Incentive amount received: {}", incentiveAmount);
                } else {
                    logger.warn("Incentive API returned null response");
                }
            } catch (RestClientException e) {
                logger.error("Error calling incentive API: {}", e.getMessage());
                logger.error("Full error: ", e);
                // Continue processing even if incentive API fails
            }

            // Update balances
            sender.setBalance(sender.getBalance() - tx.getAmount());
            recipient.setBalance(recipient.getBalance() + tx.getAmount() + incentiveAmount);

            // Save updated users
            userRepository.save(sender);
            userRepository.save(recipient);

            // Create and save transaction record with incentive
            TransactionRecord record = new TransactionRecord(sender, recipient, tx.getAmount());
            record.setIncentive(incentiveAmount);
            recordRepository.save(record);

            logger.info("Transaction processed successfully: {} -> {} : {} (incentive: {})",
                    sender.getName(), recipient.getName(), tx.getAmount(), incentiveAmount);

        } catch (Exception e) {
            logger.error("Error processing transaction", e);
            throw e;
        }
    }

    // Inner class to deserialize the incentive API response
    public static class Incentive {
        private double amount;

        public Incentive() {}

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }
    }
}