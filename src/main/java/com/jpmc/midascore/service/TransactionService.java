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

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final UserRepository userRepository;
    private final TransactionRecordRepository recordRepository;

    public TransactionService(UserRepository userRepository,
                              TransactionRecordRepository recordRepository) {
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
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

            // All validations passed - process the transaction
            // Update balances
            sender.setBalance(sender.getBalance() - tx.getAmount());
            recipient.setBalance(recipient.getBalance() + tx.getAmount());

            // Save updated users
            userRepository.save(sender);
            userRepository.save(recipient);

            // Create and save transaction record
            TransactionRecord record = new TransactionRecord(sender, recipient, tx.getAmount());
            recordRepository.save(record);

            logger.info("Transaction processed successfully: {} -> {} : {}",
                    sender.getName(), recipient.getName(), tx.getAmount());

        } catch (Exception e) {
            logger.error("Error processing transaction", e);
            throw e;
        }
    }
}