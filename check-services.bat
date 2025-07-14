@echo off
echo Checking TransactGrid services status...

echo.
echo === Docker Containers Status ===
docker ps --filter "name=transactgrid" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo.
echo === Health Checks ===

echo Checking Cassandra...
docker exec transactgrid-cassandra cqlsh -e "describe keyspaces" > nul 2>&1
if %errorlevel% == 0 (
    echo [✓] Cassandra is running
) else (
    echo [✗] Cassandra is not responding
)

echo Checking Redis...
docker exec transactgrid-redis redis-cli ping > nul 2>&1
if %errorlevel% == 0 (
    echo [✓] Redis is running
) else (
    echo [✗] Redis is not responding
)

echo Checking Elasticsearch...
curl -s -f http://localhost:9200/_cluster/health > nul 2>&1
if %errorlevel% == 0 (
    echo [✓] Elasticsearch is running
    echo    Cluster health: 
    curl -s http://localhost:9200/_cluster/health | findstr "status"
) else (
    echo [✗] Elasticsearch is not responding
)

echo.
echo === Quick Start Commands ===
echo To start all services: docker-compose up -d
echo To view logs: docker-compose logs -f [service-name]
echo To restart Elasticsearch: docker-compose restart elasticsearch

pause