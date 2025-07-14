package com.transactgrid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for TransactGrid - Scalable Transaction Processing System
 * 
 * This application provides:
 * - High-throughput transaction processing with Cassandra
 * - Real-time search capabilities with Elasticsearch
 * - Caching and rate limiting with Redis
 * - RESTful API endpoints for transaction management

 */
@SpringBootApplication(exclude = {
    CassandraDataAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    RedisAutoConfiguration.class
})
@EnableAsync
@EnableScheduling
public class TransactGridApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactGridApplication.class, args);
    }
}
