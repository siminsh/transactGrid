package com.transactgrid.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Service for managing Elasticsearch indices
 * Handles index creation and mapping setup
 */
@Service
public class ElasticsearchIndexService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchIndexService.class);

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Value("${transactgrid.elasticsearch.index.name:transactions}")
    private String indexName;

    /**
     * Initialize indices after application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndices() {
        try {
            createTransactionIndexIfNotExists();
            logger.info("Elasticsearch indices initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Elasticsearch indices", e);
        }
    }

    /**
     * Create the transaction index with proper mappings if it doesn't exist
     */
    public void createTransactionIndexIfNotExists() throws IOException {
        if (!indexExists(indexName)) {
            createTransactionIndex();
            logger.info("Created Elasticsearch index: {}", indexName);
        } else {
            logger.debug("Elasticsearch index already exists: {}", indexName);
        }
    }

    /**
     * Check if an index exists
     */
    private boolean indexExists(String indexName) throws IOException {
        ExistsRequest request = ExistsRequest.of(e -> e.index(indexName));
        return elasticsearchClient.indices().exists(request).value();
    }

    /**
     * Create the transaction index with appropriate mappings
     */
    private void createTransactionIndex() throws IOException {
        TypeMapping mapping = TypeMapping.of(m -> m
            .properties(Map.of(
                "transactionId", Property.of(p -> p.keyword(k -> k)),
                "userId", Property.of(p -> p.keyword(k -> k)),
                "amount", Property.of(p -> p.double_(d -> d)),
                "currency", Property.of(p -> p.keyword(k -> k)),
                "timestamp", Property.of(p -> p.date(d -> d.format("strict_date_optional_time"))),
                "description", Property.of(p -> p.text(t -> t
                    .analyzer("standard")
                    .fields(Map.of("keyword", Property.of(pf -> pf.keyword(k -> k))))
                )),
                "tags", Property.of(p -> p.keyword(k -> k))
            ))
        );

        CreateIndexRequest request = CreateIndexRequest.of(c -> c
            .index(indexName)
            .mappings(mapping)
            .settings(s -> s
                .numberOfShards("1")
                .numberOfReplicas("0")
                .refreshInterval(Time.of(t -> t.time("1s")))
            )
        );

        elasticsearchClient.indices().create(request);
    }

    /**
     * Recreate the index (for development/testing purposes)
     */
    public void recreateIndex() throws IOException {
        if (indexExists(indexName)) {
            elasticsearchClient.indices().delete(d -> d.index(indexName));
            logger.info("Deleted existing index: {}", indexName);
        }
        createTransactionIndex();
        logger.info("Recreated index: {}", indexName);
    }
}