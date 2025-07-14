package com.transactgrid.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.transactgrid.model.Transaction;
import com.transactgrid.model.TransactionSummary;
import com.transactgrid.repository.TransactionRepository;
import com.transactgrid.repository.TransactionSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service layer for transaction operations
 * Orchestrates between Cassandra, Elasticsearch, and Redis
 */
@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionSearchRepository transactionSearchRepository;

    @Autowired
    private CacheService cacheService;

    @Value("${transactgrid.search.default-size:50}")
    private int defaultSearchSize;
    /**
     * Save a new transaction
     * Stores in Cassandra and indexes in Elasticsearch asynchronously
     */
    public Transaction saveTransaction(Transaction transaction) {
        try {
            // Set ID and timestamp if not already set
            if (transaction.getTransactionId() == null) {
                transaction.setTransactionId(Uuids.timeBased());
            }
            if (transaction.getTimestamp() == null) {
                transaction.setTimestamp(Instant.now());
            }

            logger.info("Saving transaction: {} for user: {}", 
                transaction.getTransactionId(), transaction.getUserId());

            // Save to Cassandra (primary storage)
            transactionRepository.save(transaction);

            // Index in Elasticsearch asynchronously for search
            CompletableFuture.runAsync(() -> {
                try {
                    transactionSearchRepository.indexTransaction(transaction);
                    logger.debug("Transaction indexed in Elasticsearch: {}", transaction.getTransactionId());
                } catch (Exception e) {
                    logger.error("Failed to index transaction in Elasticsearch: {}", 
                        transaction.getTransactionId(), e);
                }
            });

            logger.info("Transaction saved successfully: {}", transaction.getTransactionId());
            return transaction;

        } catch (Exception e) {
            logger.error("Error saving transaction for user: {}", transaction.getUserId(), e);
            throw new RuntimeException("Failed to save transaction", e);
        }
    }

    /**
     * Search transactions by query string
     * Uses cache-aside pattern with Redis
     */
    public List<Transaction> searchTransactions(String query, Integer size) {
        int searchSize = size != null ? size : defaultSearchSize;
        
        try {
            // Check cache first
            Optional<List<Transaction>> cachedResults = cacheService.getCachedSearchResults(query);
            if (cachedResults.isPresent()) {
                logger.debug("Returning cached search results for query: {}", query);
                return cachedResults.get();
            }

            // Search in Elasticsearch
            logger.debug("Searching transactions with query: {} (size: {})", query, searchSize);
            List<Transaction> transactions = transactionSearchRepository.searchTransactions(query, searchSize);

            // Cache the results
            cacheService.cacheSearchResults(query, transactions);

            logger.info("Search completed for query: {} - found {} transactions", query, transactions.size());
            return transactions;

        } catch (Exception e) {
            logger.error("Error searching transactions with query: {}", query, e);
            throw new RuntimeException("Failed to search transactions", e);
        }
    }
    /**
     * Get transaction summary with aggregations
     * Uses cache-aside pattern for expensive aggregations
     */
    public List<TransactionSummary> getTransactionSummary() {
        try {
            logger.info("Getting transaction summary");
            
            // Check cache first
            Optional<List<TransactionSummary>> cachedSummary = cacheService.getCachedTransactionSummary();
            if (cachedSummary.isPresent()) {
                logger.debug("Returning cached transaction summary");
                return cachedSummary.get();
            }

            logger.debug("Cache miss for transaction summaries");

            // Get summary from Elasticsearch
            logger.debug("Computing transaction summary from Elasticsearch");
            List<TransactionSummary> summaries = transactionSearchRepository.getTransactionSummary();

            // Handle empty results gracefully
            if (summaries.isEmpty()) {
                logger.info("No transaction data available for summary");
                return new ArrayList<>();
            }

            // Cache the results
            cacheService.cacheTransactionSummary(summaries);

            logger.info("Transaction summary computed - {} user summaries", summaries.size());
            return summaries;

        } catch (Exception e) {
            logger.error("Error getting transaction summary", e);
            // Return empty list instead of throwing exception for better user experience
            logger.warn("Returning empty transaction summary due to error");
            return new ArrayList<>();
        }
    }

    /**
     * Get transactions for a specific user
     */
    public List<Transaction> getTransactionsByUserId(String userId, Integer limit) {
        int searchLimit = limit != null ? limit : defaultSearchSize;
        
        try {
            logger.debug("Getting transactions for user: {} (limit: {})", userId, searchLimit);
            
            // Get from Cassandra for accurate and consistent data
            List<Transaction> transactions = transactionRepository.findByUserId(userId, searchLimit);
            
            logger.info("Found {} transactions for user: {}", transactions.size(), userId);
            return transactions;

        } catch (Exception e) {
            logger.error("Error getting transactions for user: {}", userId, e);
            throw new RuntimeException("Failed to get user transactions", e);
        }
    }

    /**
     * Get a specific transaction by user ID and transaction ID
     */
    public Optional<Transaction> getTransaction(String userId, UUID transactionId) {
        try {
            logger.debug("Getting transaction: {} for user: {}", transactionId, userId);
            return transactionRepository.findByUserIdAndTransactionId(userId, transactionId);
        } catch (Exception e) {
            logger.error("Error getting transaction: {} for user: {}", transactionId, userId, e);
            throw new RuntimeException("Failed to get transaction", e);
        }
    }
    /**
     * Get transactions for a user within a time range
     */
    public List<Transaction> getTransactionsByUserIdAndTimeRange(String userId, Instant startTime, 
                                                                Instant endTime, Integer limit) {
        int searchLimit = limit != null ? limit : defaultSearchSize;
        
        try {
            logger.debug("Getting transactions for user: {} from {} to {} (limit: {})", 
                userId, startTime, endTime, searchLimit);
            
            List<Transaction> transactions = transactionRepository.findByUserIdAndTimeRange(
                userId, startTime, endTime, searchLimit);
            
            logger.info("Found {} transactions for user: {} in time range", transactions.size(), userId);
            return transactions;

        } catch (Exception e) {
            logger.error("Error getting transactions for user: {} in time range", userId, e);
            throw new RuntimeException("Failed to get transactions by time range", e);
        }
    }

    /**
     * Search transactions by user ID using Elasticsearch
     */
    public List<Transaction> searchTransactionsByUserId(String userId, Integer size) {
        int searchSize = size != null ? size : defaultSearchSize;
        
        try {
            logger.debug("Searching transactions for user: {} (size: {})", userId, searchSize);
            
            List<Transaction> transactions = transactionSearchRepository.searchTransactionsByUserId(userId, searchSize);
            
            logger.info("Search completed for user: {} - found {} transactions", userId, transactions.size());
            return transactions;

        } catch (Exception e) {
            logger.error("Error searching transactions for user: {}", userId, e);
            throw new RuntimeException("Failed to search user transactions", e);
        }
    }
}