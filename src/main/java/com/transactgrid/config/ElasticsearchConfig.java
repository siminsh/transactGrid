package com.transactgrid.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch configuration for real-time search and analytics.
 * 
 * Configuration includes:
 * - REST client with proper timeout settings
 * - Connection pooling for optimal performance
 * - Authentication support for secured clusters
 * - Custom JSON mapping for transaction data
 * 
 *
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.transactgrid.repository")
public class ElasticsearchConfig {

    @Value("${transactgrid.elasticsearch.hosts:localhost:9200}")
    private String[] hosts;

    @Value("${transactgrid.elasticsearch.username:}")
    private String username;

    @Value("${transactgrid.elasticsearch.password:}")
    private String password;

    @Value("${transactgrid.elasticsearch.connection.timeout:10000}")
    private int connectionTimeout;

    @Value("${transactgrid.elasticsearch.socket-timeout:60000}")
    private int socketTimeout;
    @Value("${transactgrid.elasticsearch.connection.max-connections:10}")
    private int maxConnections;

    @Value("${transactgrid.elasticsearch.max-connections-per-route:5}")
    private int maxConnectionsPerRoute;

    @Bean
    @Primary
    public RestClient restClient() {
        HttpHost[] httpHosts = new HttpHost[hosts.length];
        for (int i = 0; i < hosts.length; i++) {
            String[] hostPort = hosts[i].split(":");
            String host = hostPort[0];
            int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 9200;
            httpHosts[i] = new HttpHost(host, port, "http");
        }

        RestClientBuilder builder = RestClient.builder(httpHosts)
            .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout))
            .setHttpClientConfigCallback(httpClientBuilder -> {
                httpClientBuilder
                    .setMaxConnTotal(maxConnections)
                    .setMaxConnPerRoute(maxConnectionsPerRoute);

                if (!username.isEmpty() && !password.isEmpty()) {
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }

                return httpClientBuilder;
            });

        return builder.build();
    }

    @Bean
    public ObjectMapper elasticsearchObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register the JavaTimeModule for Java 8 time types support
        mapper.registerModule(new JavaTimeModule());
        // Configure to write dates as strings instead of timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient(RestClient restClient, ObjectMapper elasticsearchObjectMapper) {
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(elasticsearchObjectMapper));
        return new ElasticsearchClient(transport);
    }
}