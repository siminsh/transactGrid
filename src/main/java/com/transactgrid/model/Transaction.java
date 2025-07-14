package com.transactgrid.model;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Transaction model representing a financial transaction
 * Stored in Cassandra and indexed in Elasticsearch
 */
public class Transaction {

    @JsonProperty("transactionId")
    private UUID transactionId;

    @JsonProperty("userId")
    @NotBlank(message = "User ID is required")
    private String userId;

    @JsonProperty("amount")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "1000000.00", message = "Amount cannot exceed 1,000,000")
    private BigDecimal amount;

    @JsonProperty("currency")
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(USD|EUR|GBP|JPY|CAD)$", message = "Currency must be one of: USD, EUR, GBP, JPY, CAD")
    private String currency;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    @JsonProperty("description")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @JsonProperty("tags")
    @Size(max = 10, message = "Cannot have more than 10 tags")
    private Set<String> tags;

    // Constructors
    public Transaction() {
        this.transactionId = Uuids.timeBased();
        this.timestamp = Instant.now();
    }

    public Transaction(String userId, BigDecimal amount, String currency, String description, Set<String> tags) {
        this();
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.tags = tags;
    }

    // Getters and Setters
    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", timestamp=" + timestamp +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                '}';
    }
}