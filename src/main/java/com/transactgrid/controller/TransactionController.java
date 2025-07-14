package com.transactgrid.controller;

import com.transactgrid.model.Transaction;
import com.transactgrid.model.TransactionSummary;
import com.transactgrid.service.RateLimitService;
import com.transactgrid.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for Transaction operations
 * Handles API endpoints for transaction processing, search, and analytics
 */
@RestController
@RequestMapping("/transactions")
@Validated
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private RateLimitService rateLimitService;
    /**
     * POST /transactions - Register a new transaction
     */
    @PostMapping
    public ResponseEntity<?> createTransaction(@Valid @RequestBody Transaction transaction) {
        String userId = transaction.getUserId();
        
        try {
            // Check rate limiting
            if (!rateLimitService.isAllowed(userId)) {
                logger.warn("Rate limit exceeded for user: {}", userId);
                
                Map<String, Object> rateLimitInfo = new HashMap<>();
                rateLimitInfo.put("error", "Rate limit exceeded");
                rateLimitInfo.put("message", "Too many requests. Please try again later.");
                rateLimitInfo.put("remainingRequests", rateLimitService.getRemainingRequests(userId));
                rateLimitInfo.put("timeUntilReset", rateLimitService.getTimeUntilReset(userId));
                
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(rateLimitInfo);
            }

            logger.info("Creating transaction for user: {}", userId);
            Transaction savedTransaction = transactionService.saveTransaction(transaction);
            
            logger.info("Transaction created successfully: {} for user: {}", 
                savedTransaction.getTransactionId(), userId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTransaction);

        } catch (Exception e) {
            logger.error("Error creating transaction for user: {}", userId, e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", "Failed to create transaction");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * GET /transactions/search?q=query - Search transactions by keyword
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchTransactions(
            @RequestParam("q") String query,
            @RequestParam(value = "size", required = false) 
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 1000, message = "Size cannot exceed 1000") Integer size) {
        
        try {
            logger.info("Searching transactions with query: '{}' (size: {})", query, size);
            
            List<Transaction> transactions = transactionService.searchTransactions(query, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("totalResults", transactions.size());
            response.put("transactions", transactions);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error searching transactions with query: {}", query, e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Search failed");
            errorResponse.put("message", "Failed to search transactions");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    /**
     * GET /transactions/summary - Get transaction summary with aggregations
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getTransactionSummary() {
        try {
            logger.info("Getting transaction summary");
            
            List<TransactionSummary> summaries = transactionService.getTransactionSummary();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalUsers", summaries.size());
            response.put("summaries", summaries);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting transaction summary", e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Summary failed");
            errorResponse.put("message", "Failed to get transaction summary");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    }