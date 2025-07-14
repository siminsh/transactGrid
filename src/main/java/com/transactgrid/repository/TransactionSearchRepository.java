package com.transactgrid.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transactgrid.model.Transaction;
import com.transactgrid.model.TransactionSummary;
import com.transactgrid.service.ElasticsearchIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository for Transaction search operations in Elasticsearch
 * Handles indexing, searching, and aggregations
 */
@Repository
public class TransactionSearchRepository {

    private static final Logger logger = LoggerFactory.getLogger(TransactionSearchRepository.class);

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ElasticsearchIndexService indexService;

    @Value("${transactgrid.elasticsearch.index.name:transactions}")
    private String indexName;

    /**
     * Index a transaction for search and analytics
     */
    public void indexTransaction(Transaction transaction) {
        try {
            // Ensure index exists before indexing
            indexService.createTransactionIndexIfNotExists();
            
            Map<String, Object> transactionMap = convertTransactionToMap(transaction);
            
            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index(indexName)
                .id(transaction.getTransactionId().toString())
                .document(transactionMap)
            );

            elasticsearchClient.index(request);
            logger.debug("Transaction indexed successfully: {}", transaction.getTransactionId());
        } catch (IOException e) {
            logger.error("Error indexing transaction: {}", transaction.getTransactionId(), e);
            throw new RuntimeException("Failed to index transaction", e);
        }
    }

    /**
     * Search transactions by query string
     */
    public List<Transaction> searchTransactions(String queryString, int size) {
        try {
            // Check if index has any documents
            long documentCount = getDocumentCount();
            if (documentCount == 0) {
                logger.info("No transactions found in index for search query: {}", queryString);
                return new ArrayList<>();
            }

            Query query = Query.of(q -> q
                .multiMatch(m -> m
                    .query(queryString)
                    .fields("description", "tags", "currency")
                    .fuzziness("AUTO")
                )
            );

            SearchRequest request = SearchRequest.of(s -> s
                .index(indexName)
                .query(query)
                .size(size)
                .sort(sort -> sort.field(f -> f.field("timestamp").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
            );

            SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);
            
            return response.hits().hits().stream()
                    .map(hit -> convertMapToTransaction(hit.source()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error searching transactions with query: {}", queryString, e);
            throw new RuntimeException("Failed to search transactions", e);
        }
    }

    /**
     * Get transaction summary with aggregations
     */
    public List<TransactionSummary> getTransactionSummary() {
        try {
            // First check if index exists and has documents
            long documentCount = getDocumentCount();
            if (documentCount == 0) {
                logger.info("No transactions found in index, returning empty summary");
                return new ArrayList<>();
            }

            Map<String, Aggregation> aggregations = Map.of(
                "users", Aggregation.of(a -> a
                    .terms(t -> t.field("userId"))
                    .aggregations("total_amount", Aggregation.of(sub -> sub
                        .sum(sum -> sum.field("amount"))
                    ))
                    .aggregations("avg_amount", Aggregation.of(sub -> sub
                        .avg(avg -> avg.field("amount"))
                    ))
                    .aggregations("max_amount", Aggregation.of(sub -> sub
                        .max(max -> max.field("amount"))
                    ))
                    .aggregations("min_amount", Aggregation.of(sub -> sub
                        .min(min -> min.field("amount"))
                    ))
                    .aggregations("currencies", Aggregation.of(sub -> sub
                        .terms(terms -> terms.field("currency"))
                        .aggregations("currency_total", Aggregation.of(curr -> curr
                            .sum(sum -> sum.field("amount"))
                        ))
                    ))
                    .aggregations("top_tags", Aggregation.of(sub -> sub
                        .terms(terms -> terms.field("tags").size(5))
                    ))
                )
            );

            SearchRequest request = SearchRequest.of(s -> s
                .index(indexName)
                .size(0)
                .aggregations(aggregations)
            );

            SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);
            
            return parseTransactionSummary(response);
        } catch (IOException e) {
            logger.error("Error getting transaction summary", e);
            throw new RuntimeException("Failed to get transaction summary", e);
        }
    }

    /**
     * Get the total number of documents in the index
     */
    private long getDocumentCount() throws IOException {
        try {
            SearchRequest countRequest = SearchRequest.of(s -> s
                .index(indexName)
                .size(0)
                .trackTotalHits(th -> th.enabled(true))
            );
            
            SearchResponse<Map> response = elasticsearchClient.search(countRequest, Map.class);
            return response.hits().total().value();
        } catch (Exception e) {
            logger.warn("Could not get document count from index {}, assuming empty: {}", indexName, e.getMessage());
            return 0;
        }
    }

    /**
     * Search transactions by user ID
     */
    public List<Transaction> searchTransactionsByUserId(String userId, int size) {
        try {
            // Check if index has any documents
            long documentCount = getDocumentCount();
            if (documentCount == 0) {
                logger.info("No transactions found in index for user: {}", userId);
                return new ArrayList<>();
            }

            Query query = Query.of(q -> q
                .term(t -> t.field("userId").value(userId))
            );

            SearchRequest request = SearchRequest.of(s -> s
                .index(indexName)
                .query(query)
                .size(size)
                .sort(sort -> sort.field(f -> f.field("timestamp").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
            );

            SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);
            
            return response.hits().hits().stream()
                    .map(hit -> convertMapToTransaction(hit.source()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error searching transactions for user: {}", userId, e);
            throw new RuntimeException("Failed to search transactions", e);
        }
    }

    /**
     * Convert Transaction to Map for Elasticsearch indexing
     */
    private Map<String, Object> convertTransactionToMap(Transaction transaction) {
        Map<String, Object> map = new HashMap<>();
        map.put("transactionId", transaction.getTransactionId().toString());
        map.put("userId", transaction.getUserId());
        map.put("amount", transaction.getAmount());
        map.put("currency", transaction.getCurrency());
        map.put("timestamp", transaction.getTimestamp());
        map.put("description", transaction.getDescription());
        map.put("tags", transaction.getTags());
        return map;
    }

    /**
     * Convert Map from Elasticsearch to Transaction
     */
    private Transaction convertMapToTransaction(Map<String, Object> map) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.fromString((String) map.get("transactionId")));
        transaction.setUserId((String) map.get("userId"));
        transaction.setAmount(new BigDecimal(map.get("amount").toString()));
        transaction.setCurrency((String) map.get("currency"));
        transaction.setTimestamp(java.time.Instant.parse((String) map.get("timestamp")));
        transaction.setDescription((String) map.get("description"));
        
        @SuppressWarnings("unchecked")
        List<String> tagsList = (List<String>) map.get("tags");
        transaction.setTags(tagsList != null ? new HashSet<>(tagsList) : new HashSet<>());
        
        return transaction;
    }

    /**
     * Parse transaction summary from Elasticsearch aggregation response
     */
    private List<TransactionSummary> parseTransactionSummary(SearchResponse<Map> response) {
        List<TransactionSummary> summaries = new ArrayList<>();
        
        if (response.aggregations() != null && response.aggregations().containsKey("users")) {
            StringTermsAggregate usersAgg = response.aggregations().get("users").sterms();
            
            for (StringTermsBucket bucket : usersAgg.buckets().array()) {
                TransactionSummary summary = new TransactionSummary();
                summary.setUserId(bucket.key().stringValue());
                summary.setTotalTransactions(bucket.docCount());
                
                // Extract nested aggregations
                if (bucket.aggregations().containsKey("total_amount")) {
                    double totalAmount = bucket.aggregations().get("total_amount").sum().value();
                    summary.setTotalAmountByCurrency(Map.of("TOTAL", BigDecimal.valueOf(totalAmount)));
                }
                
                if (bucket.aggregations().containsKey("avg_amount")) {
                    double avgAmount = bucket.aggregations().get("avg_amount").avg().value();
                    summary.setAverageAmount(BigDecimal.valueOf(avgAmount));
                }
                
                if (bucket.aggregations().containsKey("max_amount")) {
                    double maxAmount = bucket.aggregations().get("max_amount").max().value();
                    summary.setMaxAmount(BigDecimal.valueOf(maxAmount));
                }
                
                if (bucket.aggregations().containsKey("min_amount")) {
                    double minAmount = bucket.aggregations().get("min_amount").min().value();
                    summary.setMinAmount(BigDecimal.valueOf(minAmount));
                }
                
                // Extract currency distribution
                if (bucket.aggregations().containsKey("currencies")) {
                    StringTermsAggregate currenciesAgg = bucket.aggregations().get("currencies").sterms();
                    Map<String, BigDecimal> currencyTotals = new HashMap<>();
                    String mostUsedCurrency = null;
                    long maxCurrencyCount = 0;
                    
                    for (StringTermsBucket currencyBucket : currenciesAgg.buckets().array()) {
                        String currency = currencyBucket.key().stringValue();
                        if (currencyBucket.aggregations().containsKey("currency_total")) {
                            double amount = currencyBucket.aggregations().get("currency_total").sum().value();
                            currencyTotals.put(currency, BigDecimal.valueOf(amount));
                        }
                        
                        if (currencyBucket.docCount() > maxCurrencyCount) {
                            maxCurrencyCount = currencyBucket.docCount();
                            mostUsedCurrency = currency;
                        }
                    }
                    
                    summary.setTotalAmountByCurrency(currencyTotals);
                    summary.setMostUsedCurrency(mostUsedCurrency);
                }
                
                // Extract top tags
                if (bucket.aggregations().containsKey("top_tags")) {
                    StringTermsAggregate tagsAgg = bucket.aggregations().get("top_tags").sterms();
                    Map<String, Long> topTags = new HashMap<>();
                    
                    for (StringTermsBucket tagBucket : tagsAgg.buckets().array()) {
                        topTags.put(tagBucket.key().stringValue(), tagBucket.docCount());
                    }
                    
                    summary.setTopTags(topTags);
                }
                
                summaries.add(summary);
            }
        }
        
        return summaries;
    }
}
