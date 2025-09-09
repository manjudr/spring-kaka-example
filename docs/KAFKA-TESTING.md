# Kafka Testing and Debugging Guide

## 1. Running the Application
```bash
# Start all services in background
docker compose up -d

# View logs of all services
docker compose logs -f

# View logs of specific service
docker compose logs -f app    # Application logs
docker compose logs -f kafka  # Kafka broker logs
```

## 2. Producing Test Messages

### 2.1 Using Kafka Console Producer
```bash
# Enter kafka container
docker exec -it kafka-schema-pipeline-kafka-1 bash

# Produce message to input topic
kafka-console-producer --bootstrap-server localhost:9092 --topic events.input
```

Example valid JSON message:
```json
{
  "context": {
    "domain": "retail",
    "action": "search",
    "timestamp": "2023-09-02T10:00:00Z"
  },
  "type": "NOTIFICATION",
  "payload": {
    "query": "example search"
  }
}
```

### 2.2 Using curl (if REST endpoint is available)
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "context": {
      "domain": "retail",
      "action": "search",
      "timestamp": "2023-09-02T10:00:00Z"
    },
    "type": "NOTIFICATION",
    "payload": {
      "query": "example search"
    }
  }'
```

## 3. Consuming Messages

### 3.1 Using Kafka Console Consumer
```bash
# Enter kafka container
docker exec -it kafka-schema-pipeline-kafka-1 bash

# Consume from beginning of topic
kafka-console-consumer --bootstrap-server localhost:9092 \
                      --topic events.input \
                      --from-beginning

# Consume with consumer group
kafka-console-consumer --bootstrap-server localhost:9092 \
                      --topic events.input \
                      --group test-group
```

### 3.2 View Consumer Group Details
```bash
# List consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Describe specific consumer group
kafka-consumer-groups --bootstrap-server localhost:9092 \
                     --describe \
                     --group schema-pipeline-group
```

## 4. Debugging

### 4.1 View Application Logs
```bash
# View application logs
docker compose logs -f app

# View kafka logs
docker compose logs -f kafka
```

### 4.2 Check Consumer Lag
```bash
# Check consumer group lag
kafka-consumer-groups --bootstrap-server localhost:9092 \
                     --describe \
                     --group schema-pipeline-group
```

Sample output:
```
GROUP                TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
schema-pipeline-group events.input    0         1              1               0
schema-pipeline-group events.input    1         2              2               0
schema-pipeline-group events.input    2         1              1               0
```

### 4.3 List Topics
```bash
kafka-topics --bootstrap-server localhost:9092 --list
```

### 4.4 Describe Topic Details
```bash
kafka-topics --bootstrap-server localhost:9092 \
            --describe \
            --topic events.input
```

## 5. Common Debugging Scenarios

### 5.1 Message Not Being Processed
1. Check if message was produced correctly:
```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
                      --topic events.input \
                      --from-beginning
```

2. Check consumer group status:
```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
                     --describe \
                     --group schema-pipeline-group
```

3. Check application logs:
```bash
docker compose logs -f app
```

### 5.2 Schema Validation Failures
1. Check application logs for validation errors:
```bash
docker compose logs -f app | grep "Schema validation failed"
```

2. Verify message format using console consumer:
```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
                      --topic events.input \
                      --from-beginning \
                      --property print.key=true \
                      --property print.value=true
```

### 5.3 Consumer Group Rebalancing
1. Monitor consumer group events:
```bash
docker compose logs -f app | grep "Rebalance"
```

2. Check partition assignments:
```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
                     --describe \
                     --group schema-pipeline-group \
                     --members --verbose
```

## 6. Monitoring Tools

### 6.1 Topic Management
```bash
# List all topics
kafka-topics --bootstrap-server localhost:9092 --list

# View topic details
kafka-topics --bootstrap-server localhost:9092 --describe --topic events.input
```

### 6.2 Partition Management
```bash
# Add partitions to a topic
kafka-topics --bootstrap-server localhost:9092 \
            --alter \
            --topic events.input \
            --partitions 4
```

This guide provides the essential commands and procedures for testing and debugging your Kafka application. Keep it handy for troubleshooting and verification purposes.
