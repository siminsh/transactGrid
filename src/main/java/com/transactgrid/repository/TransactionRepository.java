package com.transactgrid.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.transactgrid.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository for Transaction operations in Cassandra
 * Handles CRUD operations with proper partitioning by userId
 */
@Repository
public class TransactionRepository {

    private static final Logger logger = LoggerFactory.getLogger(TransactionRepository.class);

    @Autowired
    private CqlSession cqlSession;

    // Prepared statements for better performance
    private PreparedStatement insertStatement;
    private PreparedStatement selectByUserStatement;
    private PreparedStatement selectByUserAndIdStatement;
    private PreparedStatement selectByUserAndTimeRangeStatement;

    @PostConstruct
    public void init() {
        // Initialize prepared statements
        this.insertStatement = cqlSession.prepare(
            "INSERT INTO transactions (user_id, transaction_id, amount, currency, timestamp, description, tags) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)"
        );

        this.selectByUserStatement = cqlSession.prepare(
            "SELECT * FROM transactions WHERE user_id = ? LIMIT ?"
        );

        this.selectByUserAndIdStatement = cqlSession.prepare(
            "SELECT * FROM transactions WHERE user_id = ? AND transaction_id = ?"
        );

        this.selectByUserAndTimeRangeStatement = cqlSession.prepare(
                "SELECT * FROM transactions WHERE user_id = ? AND timestamp >= ? AND timestamp <= ? LIMIT ? ALLOW FILTERING"
        );

    }

    /**
     * Save a transaction to Cassandra
     */
    public void save(Transaction transaction) {
        try {
            BoundStatement boundStatement = insertStatement.bind(
                transaction.getUserId(),
                transaction.getTransactionId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getTimestamp(),
                transaction.getDescription(),
                transaction.getTags()
            ).setConsistencyLevel(ConsistencyLevel.QUORUM);

            cqlSession.execute(boundStatement);
            logger.debug("Transaction saved successfully: {}", transaction.getTransactionId());
        } catch (Exception e) {
            logger.error("Error saving transaction: {}", transaction.getTransactionId(), e);
            throw new RuntimeException("Failed to save transaction", e);
        }
    }

    /**
     * Find transactions by user ID with limit
     */
    public List<Transaction> findByUserId(String userId, int limit) {
        try {
            BoundStatement boundStatement = selectByUserStatement.bind(userId, limit)
                .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
            ResultSet resultSet = cqlSession.execute(boundStatement);
            
            return resultSet.all().stream()
                    .map(this::mapRowToTransaction)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error finding transactions for user: {}", userId, e);
            throw new RuntimeException("Failed to find transactions", e);
        }
    }

    /**
     * Find a specific transaction by user ID and transaction ID
     */
    public Optional<Transaction> findByUserIdAndTransactionId(String userId, UUID transactionId) {
        try {
            BoundStatement boundStatement = selectByUserAndIdStatement.bind(userId, transactionId)
                .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
            ResultSet resultSet = cqlSession.execute(boundStatement);
            
            Row row = resultSet.one();
            if (row != null) {
                return Optional.of(mapRowToTransaction(row));
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error finding transaction: {} for user: {}", transactionId, userId, e);
            throw new RuntimeException("Failed to find transaction", e);
        }
    }

    /**
     * Find transactions by user ID within a time range
     */
    public List<Transaction> findByUserIdAndTimeRange(String userId, Instant startTime, Instant endTime, int limit) {
        try {
            BoundStatement boundStatement = selectByUserAndTimeRangeStatement.bind(
                userId, startTime, endTime, limit
            ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
            ResultSet resultSet = cqlSession.execute(boundStatement);
            
            return resultSet.all().stream()
                    .map(this::mapRowToTransaction)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error finding transactions for user: {} in time range: {} to {}", 
                    userId, startTime, endTime, e);
            throw new RuntimeException("Failed to find transactions", e);
        }
    }

    /**
     * Map Cassandra row to Transaction object
     */
    private Transaction mapRowToTransaction(Row row) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(row.getUuid("transaction_id"));
        transaction.setUserId(row.getString("user_id"));
        transaction.setAmount(row.getBigDecimal("amount"));
        transaction.setCurrency(row.getString("currency"));
        transaction.setTimestamp(row.getInstant("timestamp"));
        transaction.setDescription(row.getString("description"));
        
        // Handle tags set
        Set<String> tags = row.getSet("tags", String.class);
        transaction.setTags(tags != null ? tags : new HashSet<>());
        
        return transaction;
    }
}