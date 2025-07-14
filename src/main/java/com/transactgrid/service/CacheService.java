package com.transactgrid.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.transactgrid.model.Transaction;
import com.transactgrid.model.TransactionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Service for caching search results and summaries using Redis
 * Implements cache-aside pattern with TTL-based expiration
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private static final String SEARCH_CACHE_PREFIX = "search:";
    private static final String SUMMARY_CACHE_PREFIX = "summary:";
    private static final String USER_TRANSACTIONS_CACHE_PREFIX = "user_transactions:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${transactgrid.redis.cache.ttl:300}")
    private long cacheTtlSeconds;

    /**
     * Cache search results
     */
    public void cacheSearchResults(String query, List<Transaction> transactions) {
        String key = SEARCH_CACHE_PREFIX + generateCacheKey(query);
        
        try {
            String serializedTransactions = objectMapper.writeValueAsString(transactions);
            redisTemplate.opsForValue().set(key, serializedTransactions, Duration.ofSeconds(cacheTtlSeconds));
            logger.debug("Cached search results for query: {} (count: {})", query, transactions.size());
        } catch (JsonProcessingException e) {
            logger.error("Error caching search results for query: {}", query, e);
        }
    }

    /**
     * Get cached search results
     */
    public Optional<List<Transaction>> getCachedSearchResults(String query) {
        String key = SEARCH_CACHE_PREFIX + generateCacheKey(query);
        
        try {
            String cachedData = redisTemplate.opsForValue().get(key);
            if (cachedData != null) {
                List<Transaction> transactions = objectMapper.readValue(
                    cachedData, 
                    new TypeReference<List<Transaction>>() {}
                );
                logger.debug("Cache hit for search query: {} (count: {})", query, transactions.size());
                return Optional.of(transactions);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing cached search results for query: {}", query, e);
        }
        
        logger.debug("Cache miss for search query: {}", query);
        return Optional.empty();
    }

    /**
     * Cache transaction summary
     */
    public void cacheTransactionSummary(List<TransactionSummary> summaries) {
        String key = SUMMARY_CACHE_PREFIX + "all";
        
        try {
            String serializedSummaries = objectMapper.writeValueAsString(summaries);
            redisTemplate.opsForValue().set(key, serializedSummaries, Duration.ofSeconds(cacheTtlSeconds));
            logger.debug("Cached transaction summaries (count: {})", summaries.size());
        } catch (JsonProcessingException e) {
            logger.error("Error caching transaction summaries", e);
        }
    }

    /**
     * Get cached transaction summary
     */
    public Optional<List<TransactionSummary>> getCachedTransactionSummary() {
        String key = SUMMARY_CACHE_PREFIX + "all";
        
        try {
            String cachedData = redisTemplate.opsForValue().get(key);
            if (cachedData != null) {
                List<TransactionSummary> summaries = objectMapper.readValue(
                    cachedData, 
                    new TypeReference<List<TransactionSummary>>() {}
                );
                logger.debug("Cache hit for transaction summaries (count: {})", summaries.size());
                return Optional.of(summaries);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing cached transaction summaries", e);
        }
        
        logger.debug("Cache miss for transaction summaries");
        return Optional.empty();
    }

    /**
     * Cache user transactions
     */
    public void cacheUserTransactions(String userId, List<Transaction> transactions) {
        String key = USER_TRANSACTIONS_CACHE_PREFIX + userId;
        
        try {
            String serializedTransactions = objectMapper.writeValueAsString(transactions);
            redisTemplate.opsForValue().set(key, serializedTransactions, Duration.ofSeconds(cacheTtlSeconds));
            logger.debug("Cached user transactions for user: {} (count: {})", userId, transactions.size());
        } catch (JsonProcessingException e) {
            logger.error("Error caching user transactions for user: {}", userId, e);
        }
    }

    /**
     * Get cached user transactions
     */
    public Optional<List<Transaction>> getCachedUserTransactions(String userId) {
        String key = USER_TRANSACTIONS_CACHE_PREFIX + userId;
        
        try {
            String cachedData = redisTemplate.opsForValue().get(key);
            if (cachedData != null) {
                List<Transaction> transactions = objectMapper.readValue(
                    cachedData, 
                    new TypeReference<List<Transaction>>() {}
                );
                logger.debug("Cache hit for user transactions: {} (count: {})", userId, transactions.size());
                return Optional.of(transactions);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing cached user transactions for user: {}", userId, e);
        }
        
        logger.debug("Cache miss for user transactions: {}", userId);
        return Optional.empty();
    }

    /**
     * Generate cache key from query string
     * Normalizes the query to ensure consistent caching
     */
    private String generateCacheKey(String query) {
        // Normalize the query: trim, lowercase, and replace multiple spaces with single space
        String normalizedQuery = query.trim().toLowerCase().replaceAll("\\s+", " ");
        
        // Use hash for very long queries to avoid Redis key length issues
        if (normalizedQuery.length() > 100) {
            return String.valueOf(normalizedQuery.hashCode());
        }
        
        // Replace spaces and special characters with underscores for safe Redis keys
        return normalizedQuery.replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * Clear all cached data (admin function)
     */
    public void clearAllCache() {
        try {
            // Clear search cache
            redisTemplate.delete(redisTemplate.keys(SEARCH_CACHE_PREFIX + "*"));
            
            // Clear summary cache
            redisTemplate.delete(redisTemplate.keys(SUMMARY_CACHE_PREFIX + "*"));
            
            // Clear user transactions cache
            redisTemplate.delete(redisTemplate.keys(USER_TRANSACTIONS_CACHE_PREFIX + "*"));
            
            logger.info("All cache cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing cache", e);
        }
    }

    /**
     * Clear cache for specific user
     */
    public void clearUserCache(String userId) {
        try {
            String userTransactionsKey = USER_TRANSACTIONS_CACHE_PREFIX + userId;
            redisTemplate.delete(userTransactionsKey);
            logger.info("Cache cleared for user: {}", userId);
        } catch (Exception e) {
            logger.error("Error clearing cache for user: {}", userId, e);
        }
    }
}