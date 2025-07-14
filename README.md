# TransactGrid - Scalable Transaction Processing System

## Overview
This project is a scalable and resilient **transaction processing and search system** built with:
* **Apache Cassandra** for high-throughput distributed data storage
* **Redis** for fast caching and rate-limiting
* **Elasticsearch** for full-text search and real-time aggregation
* **Spring Boot** for REST APIs and orchestration

The system is designed to handle a large volume of transactions efficiently while enabling real-time search and analysis.

## 🏛 Architecture

```
[Client]
   |
[Spring Boot API Layer]
   |
   |-- Save Transaction --> [Cassandra (partitioned by userId)]
   |
   |-- Index for Search --> [Elasticsearch (sharded index)]
   |
   |-- Cache/Rate Limit --> [Redis]
```

## 📊 Key Features
* Store transaction data with partitioning and consistency using Cassandra
* Fast, fuzzy search and analytics via Elasticsearch
* Redis-backed caching of popular queries
* User-based rate limiting to prevent API abuse
* Dockerized environment for local development

## 📆 Technologies Used
* Java 17
* Spring Boot 3.x
* Apache Cassandra
* Redis
* Elasticsearch
* Docker & Docker Compose

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Java 17 installed
- Maven 3.8+ installed

### Running the Application

1. Clone the repository:
```bash
git clone https://github.com/your-username/transact-grid.git
cd transact-grid
```

2. Start the infrastructure services:
```bash
docker-compose up -d
```

3. Wait for services to be ready (approximately 30-60 seconds):
```bash
# Check if all services are healthy
docker-compose ps

# Or follow the logs
docker-compose logs -f
```

4. Initialize the database schema:
```bash
# Copy schema to Cassandra container and execute
docker exec -it transact-cassandra cqlsh -f /docker-entrypoint-initdb.d/schema.cql
```

5. Run the Spring Boot application:
```bash
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

### Service Endpoints
- **API**: http://localhost:8080
- **Cassandra**: localhost:9042
- **Redis**: localhost:6379
- **Elasticsearch**: http://localhost:9200
- **Kibana** (optional): http://localhost:5601

## API Endpoints

### POST /transactions
Register a transaction.

```json
{
  "userId": "user-101",
  "amount": 250.0,
  "currency": "EUR",
  "timestamp": "2025-07-11T09:00:00Z",
  "description": "Grocery at Lidl",
  "tags": ["food", "grocery"]
}
```

### GET /transactions/search?q=grocery
Search transactions by keyword (uses Elasticsearch).

### GET /transactions/summary
Get total amounts and transaction count per user (Elasticsearch aggregation).

### Health Check
GET /actuator/health - Application health status

**Example Response:**
```json
{
  "status": "UP",
  "components": {
    "cassandra": {"status": "UP"},
    "redis": {"status": "UP"},
    "elasticsearch": {"status": "UP"}
  }
}
```

## 🐳 Docker Configuration

The system uses Docker Compose to orchestrate the following services:

### docker-compose.yml
```yaml
version: '3.8'

services:
  cassandra:
    image: cassandra:4.1
    container_name: transact-cassandra
    ports:
      - "9042:9042"
    environment:
      - CASSANDRA_CLUSTER_NAME=TransactGrid
      - CASSANDRA_DC=datacenter1
      - CASSANDRA_RACK=rack1
    volumes:
      - cassandra_data:/var/lib/cassandra
      - ./src/main/resources/schema.cql:/docker-entrypoint-initdb.d/schema.cql
    networks:
      - transact-network
    healthcheck:
      test: ["CMD-SHELL", "cqlsh -e 'describe cluster'"]
      interval: 30s
      timeout: 10s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: transact-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - transact-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: transact-elasticsearch
    ports:
      - "9200:9200"
      - "9300:9300"
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    networks:
      - transact-network
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    container_name: transact-kibana
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch
    networks:
      - transact-network

volumes:
  cassandra_data:
  redis_data:
  elasticsearch_data:

networks:
  transact-network:
    driver: bridge
```

### Docker Commands
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f [service_name]

# Scale services (example: 3 Cassandra nodes)
docker-compose up -d --scale cassandra=3

# Clean up volumes (WARNING: deletes all data)
docker-compose down -v
```

## ⚡ Rate Limiting
Users are limited to 10 requests per minute. If exceeded, HTTP 429 is returned. Implemented using Redis atomic operations and expiration keys.

## 📊 Data Distribution
* **Cassandra**: Partitioned by `userId`, using QUORUM consistency
* **Elasticsearch**: Index sharded by date
* **Redis**: Keys structured as `cache:{query}` and `rate:{userId}`

## 🌟 Highlights
* Consistent hashing & partitioning strategies (Cassandra)
* Fault-tolerant architecture with replicas
* Real-time metrics with Elasticsearch aggregations
* Scalable and production-ready foundation

## 🛠 Development

### Project Structure
```
TransactGrid/
├── src/main/java/com/transactgrid/
│   ├── TransactGridApplication.java
│   ├── config/
│   │   ├── CassandraConfig.java
│   │   ├── RedisConfig.java
│   │   └── ElasticsearchConfig.java
│   ├── controller/
│   │   └── TransactionController.java
│   ├── model/
│   │   ├── Transaction.java
│   │   └── TransactionSummary.java
│   ├── repository/
│   │   ├── TransactionRepository.java
│   │   └── TransactionSearchRepository.java
│   └── service/
│       ├── TransactionService.java
│       ├── CacheService.java
│       └── RateLimitService.java
├── src/main/resources/
│   ├── application.yml
│   └── schema.cql
├── docker-compose.yml
├── pom.xml
└── README.md
```

### Database Schema

#### Cassandra Schema (schema.cql)
```sql
CREATE KEYSPACE IF NOT EXISTS transact_grid
WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': 1
};

USE transact_grid;

CREATE TABLE IF NOT EXISTS transactions (
    user_id text,
    transaction_id timeuuid,
    amount decimal,
    currency text,
    timestamp timestamp,
    description text,
    tags set<text>,
    PRIMARY KEY (user_id, transaction_id)
) WITH CLUSTERING ORDER BY (transaction_id DESC);

CREATE INDEX IF NOT EXISTS ON transactions (currency);
CREATE INDEX IF NOT EXISTS ON transactions (timestamp);
```

#### Elasticsearch Index Mapping
```json
{
  "mappings": {
    "properties": {
      "userId": {"type": "keyword"},
      "transactionId": {"type": "keyword"},
      "amount": {"type": "double"},
      "currency": {"type": "keyword"},
      "timestamp": {"type": "date"},
      "description": {"type": "text", "analyzer": "standard"},
      "tags": {"type": "keyword"}
    }
  }
}
```

### Environment Variables
The application uses the following environment variables:
- `CASSANDRA_CONTACT_POINTS`: Cassandra contact points (default: localhost:9042)
- `REDIS_HOST`: Redis host (default: localhost)
- `ELASTICSEARCH_HOSTS`: Elasticsearch hosts (default: localhost:9200)

## 🔧 Configuration
All configuration is done through `application.yml`. Key settings include:
- Database connection pools
- Cache TTL settings
- Rate limiting configuration
- Elasticsearch index settings

## 📈 Monitoring
The system includes health checks and metrics for:
- Transaction processing rates
- Cache hit ratios
- Database connection status
- Search query performance

### Metrics Endpoints
- `/actuator/health` - Overall application health
- `/actuator/metrics` - Detailed metrics
- `/actuator/info` - Application information

## 🔧 Troubleshooting

### Common Issues

#### Services Not Starting
```bash
# Check Docker daemon
docker --version
docker-compose --version

# Check port conflicts
netstat -an | grep -E "(9042|6379|9200|8080)"

# Clean restart
docker-compose down
docker system prune -f
docker-compose up -d
```

#### Connection Issues
```bash
# Test Cassandra connection
docker exec -it transact-cassandra cqlsh

# Test Redis connection
docker exec -it transact-redis redis-cli ping

# Test Elasticsearch connection
curl -X GET "localhost:9200/_cluster/health"
```

#### Performance Issues
- **Slow queries**: Check Elasticsearch indices and Cassandra partition keys
- **High memory usage**: Adjust JVM settings in docker-compose.yml
- **Rate limiting**: Monitor Redis rate limit keys

### Logging
Enable debug logging in `application.yml`:
```yaml
logging:
  level:
    com.transactgrid: DEBUG
    org.springframework.data.cassandra: DEBUG
```

## 🚀 Performance & Scaling

### Cassandra Optimization
- **Partition Strategy**: Data is partitioned by `userId` for optimal distribution
- **Consistency Level**: QUORUM for balanced consistency and performance
- **Batch Operations**: Use batch inserts for bulk transaction processing
- **Compaction**: LeveledCompactionStrategy for read-heavy workloads

### Redis Optimization
- **Memory Policy**: `allkeys-lru` for cache eviction
- **Persistence**: RDB snapshots for durability
- **Connection Pool**: Lettuce with connection pooling

### Elasticsearch Optimization
- **Sharding**: Index sharded by date for time-based queries
- **Refresh Interval**: Optimized for near real-time search
- **Bulk Operations**: Batch document indexing

### Production Deployment
```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  cassandra:
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 4G
        reservations:
          memory: 2G
  
  redis:
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M
  
  elasticsearch:
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 1G
```

## 🤝 Contributing
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License
This project is licensed under the MIT License.
