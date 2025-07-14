package com.transactgrid.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service for rate limiting using Redis
 * Implements sliding window rate limiting per user
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${transactgrid.redis.rate-limiting.requests-per-minute:10}")
    private int requestsPerMinute;

    @Value("${transactgrid.redis.rate-limiting.window-size:60}")
    private long windowSizeSeconds;

    /**
     * Check if the user is within rate limits
     * @param userId User identifier
     * @return true if within limits, false if rate limited
     */
    public boolean isAllowed(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        
        try {
            // Get current count
            String currentCountStr = redisTemplate.opsForValue().get(key);
            int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
            
            if (currentCount >= requestsPerMinute) {
                logger.debug("Rate limit exceeded for user: {} (current: {}, limit: {})", 
                    userId, currentCount, requestsPerMinute);
                return false;
            }
            
            // Increment counter
            Long newCount = redisTemplate.opsForValue().increment(key);
            
            // Set expiration if this is the first request in the window
            if (newCount == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSizeSeconds));
            }
            
            logger.debug("Rate limit check passed for user: {} (count: {}/{})", 
                userId, newCount, requestsPerMinute);
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking rate limit for user: {}", userId, e);
            // Allow request in case of Redis failure (fail-open)
            return true;
        }
    }

    /**
     * Get remaining requests for a user
     * @param userId User identifier
     * @return number of remaining requests in current window
     */
    public int getRemainingRequests(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        
        try {
            String currentCountStr = redisTemplate.opsForValue().get(key);
            int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
            return Math.max(0, requestsPerMinute - currentCount);
        } catch (Exception e) {
            logger.error("Error getting remaining requests for user: {}", userId, e);
            return requestsPerMinute; // Return max in case of error
        }
    }

    /**
     * Get time until rate limit window resets
     * @param userId User identifier
     * @return seconds until reset, or 0 if no active window
     */
    public long getTimeUntilReset(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            logger.error("Error getting TTL for user: {}", userId, e);
            return 0;
        }
    }

    /**
     * Reset rate limit for a user (admin function)
     * @param userId User identifier
     */
    public void resetRateLimit(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        
        try {
            redisTemplate.delete(key);
            logger.info("Rate limit reset for user: {}", userId);
        } catch (Exception e) {
            logger.error("Error resetting rate limit for user: {}", userId, e);
        }
    }
}
