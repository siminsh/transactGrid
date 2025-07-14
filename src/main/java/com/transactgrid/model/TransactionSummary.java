package com.transactgrid.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Transaction summary model for aggregated transaction data
 * Used for analytics and reporting
 */
public class TransactionSummary {

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("totalTransactions")
    private long totalTransactions;

    @JsonProperty("totalAmountByCurrency")
    private Map<String, BigDecimal> totalAmountByCurrency;

    @JsonProperty("averageAmount")
    private BigDecimal averageAmount;

    @JsonProperty("maxAmount")
    private BigDecimal maxAmount;

    @JsonProperty("minAmount")
    private BigDecimal minAmount;

    @JsonProperty("mostUsedCurrency")
    private String mostUsedCurrency;

    @JsonProperty("topTags")
    private Map<String, Long> topTags;

    // Constructors
    public TransactionSummary() {
    }

    public TransactionSummary(String userId, long totalTransactions, Map<String, BigDecimal> totalAmountByCurrency) {
        this.userId = userId;
        this.totalTransactions = totalTransactions;
        this.totalAmountByCurrency = totalAmountByCurrency;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public Map<String, BigDecimal> getTotalAmountByCurrency() {
        return totalAmountByCurrency;
    }

    public void setTotalAmountByCurrency(Map<String, BigDecimal> totalAmountByCurrency) {
        this.totalAmountByCurrency = totalAmountByCurrency;
    }

    public BigDecimal getAverageAmount() {
        return averageAmount;
    }

    public void setAverageAmount(BigDecimal averageAmount) {
        this.averageAmount = averageAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public String getMostUsedCurrency() {
        return mostUsedCurrency;
    }

    public void setMostUsedCurrency(String mostUsedCurrency) {
        this.mostUsedCurrency = mostUsedCurrency;
    }

    public Map<String, Long> getTopTags() {
        return topTags;
    }

    public void setTopTags(Map<String, Long> topTags) {
        this.topTags = topTags;
    }

    @Override
    public String toString() {
        return "TransactionSummary{" +
                "userId='" + userId + '\'' +
                ", totalTransactions=" + totalTransactions +
                ", totalAmountByCurrency=" + totalAmountByCurrency +
                ", averageAmount=" + averageAmount +
                ", maxAmount=" + maxAmount +
                ", minAmount=" + minAmount +
                ", mostUsedCurrency='" + mostUsedCurrency + '\'' +
                ", topTags=" + topTags +
                '}';
    }
}