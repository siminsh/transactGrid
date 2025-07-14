package com.transactgrid.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.util.Collections;
import java.util.List;

/**
 * Simplified Cassandra configuration for TransactGrid.
 * 
 *
 */
@Configuration
@EnableCassandraRepositories(basePackages = "com.transactgrid.repository")
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${transactgrid.cassandra.contact-points:127.0.0.1}")
    private String contactPoints;

    @Value("${transactgrid.cassandra.port:9042}")
    private int port;

    @Value("${transactgrid.cassandra.keyspace:transact_grid}")
    private String keyspaceName;

    @Value("${transactgrid.cassandra.datacenter:dc1}")
    private String localDatacenter;

    @Value("${transactgrid.cassandra.username:}")
    private String username;

    @Value("${transactgrid.cassandra.password:}")
    private String password;

    @Override
    protected String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    protected String getLocalDataCenter() {
        return localDatacenter;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints.split(":")[0]; // Remove port if present
    }

    @Override
    protected int getPort() {
        String[] hostPort = contactPoints.split(":");
        return hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : port;
    }

    @Override
    protected List<String> getStartupScripts() {
        return Collections.singletonList("CREATE KEYSPACE IF NOT EXISTS " + keyspaceName + 
            " WITH replication = { 'class': 'SimpleStrategy', 'replication_factor': 1 };");
    }

    @Override
    protected List<String> getShutdownScripts() {
        return Collections.emptyList();
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }
}