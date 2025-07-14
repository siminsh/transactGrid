-- TransactGrid Cassandra Schema
-- This script initializes the keyspace and tables for the TransactGrid system

-- Create keyspace with replication strategy
CREATE KEYSPACE IF NOT EXISTS transact_grid 
WITH replication = {
    'class': 'SimpleStrategy', 
    'replication_factor': 1
};

-- Use the keyspace
USE transact_grid;

-- Main transactions table partitioned by user_id for optimal distribution
-- Clustering by transaction_id (timeuuid) provides natural time ordering
CREATE TABLE IF NOT EXISTS transactions (
    user_id text,
    transaction_id timeuuid,
    amount decimal,
    currency text,
    timestamp timestamp,
    description text,
    tags set<text>,
    PRIMARY KEY (user_id, transaction_id)
) WITH CLUSTERING ORDER BY (transaction_id DESC)
  AND comment = 'Financial transactions partitioned by user for scalability'
  AND gc_grace_seconds = 864000
  AND compaction = {
    'class': 'LeveledCompactionStrategy',
    'sstable_size_in_mb': 160
  };

-- Secondary indexes for common query patterns
-- Note: Use sparingly in production, consider denormalization instead
CREATE INDEX IF NOT EXISTS ON transactions (currency);
CREATE INDEX IF NOT EXISTS ON transactions (timestamp);

-- Optional: Table for transaction summaries (pre-computed aggregations)
-- This can be used for caching expensive aggregation results
CREATE TABLE IF NOT EXISTS transaction_summaries (
    user_id text,
    period text, -- 'daily', 'monthly', 'yearly'
    period_start timestamp,
    total_transactions bigint,
    total_amount_usd decimal,
    currency_totals map<text, decimal>,
    top_tags map<text, bigint>,
    last_updated timestamp,
    PRIMARY KEY (user_id, period, period_start)
) WITH CLUSTERING ORDER BY (period ASC, period_start DESC)
  AND comment = 'Pre-computed transaction summaries for analytics';

-- Show created tables to verify setup
DESCRIBE TABLES;

-- Show keyspace info
DESCRIBE KEYSPACE transact_grid;