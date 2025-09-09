# Kafka Consumer Resiliency Testing Guide

## 1. Understanding Offset Management

### 1.1 Where Offsets Are Stored
- Offsets are stored in internal Kafka topic: `__consumer_offsets`
- Each consumer group maintains its own offset for each partition
- Our configuration:
  ```yaml
  spring:
    kafka:
      consumer:
        enable-auto-commit: false
        auto-offset-reset: earliest
  ```

### 1.2 View Current Offsets
```bash
# Check current offsets for consumer group
docker exec -it kafka-schema-pipeline-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group schema-pipeline-group
```

Expected output:
```
GROUP                TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
schema-pipeline-group events.input    0         1              1               0
schema-pipeline-group events.input    1         2              2               0
schema-pipeline-group events.input    2         1              1               0
```

## 2. Testing Scenarios

### 2.1 Exactly-Once Processing Test

1. Start with clean state:
```bash
# Stop all services
docker compose down

# Start again
docker compose up -d
```

2. Produce a test message:
```bash
docker exec -it kafka-schema-pipeline-kafka-1 kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic events.input
```
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "ts": "2023-09-02T10:00:00Z",
  "type": "CREATE",
  "payload": {
    "testId": "DUPLICATE_TEST_1"
  }
}
```

3. Check offset after processing:
```bash
docker exec -it kafka-schema-pipeline-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group schema-pipeline-group
```

4. Restart application:
```bash
docker compose restart app
```

5. Check logs to verify no reprocessing:
```bash
docker compose logs -f app
```

### 2.2 Failure Recovery Test

1. Produce multiple messages:
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174001",
  "ts": "2023-09-02T10:00:00Z",
  "type": "CREATE",
  "payload": {
    "testId": "RECOVERY_TEST_1"
  }
}
```
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174002",
  "ts": "2023-09-02T10:00:00Z",
  "type": "CREATE",
  "payload": {
    "testId": "RECOVERY_TEST_2"
  }
}
```

2. Kill application during processing:
```bash
docker compose kill app
```

3. Check current offsets:
```bash
docker exec -it kafka-schema-pipeline-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group schema-pipeline-group
```

4. Restart application:
```bash
docker compose start app
```

5. Verify processing resumes from last committed offset:
```bash
docker compose logs -f app
```

### 2.3 Partition Rebalancing Test

1. Start with multiple consumers:
```bash
docker compose up -d --scale app=3
```

2. Check partition assignment:
```bash
docker exec -it kafka-schema-pipeline-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group schema-pipeline-group \
  --members --verbose
```

3. Kill one consumer instance:
```bash
docker compose kill app-1
```

4. Check rebalancing:
```bash
docker compose logs -f | grep "Rebalance"
```

## 3. Monitoring and Verification Tools

### 3.1 Check Consumer Group Details
```bash
# List all consumer groups
docker exec -it kafka-schema-pipeline-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list

# Get detailed consumer group info
docker exec -it kafka-schema-pipeline-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group schema-pipeline-group \
  --members \
  --verbose
```

### 3.2 View Consumer Offsets Topic
```bash
# View internal offsets topic
docker exec -it kafka-schema-pipeline-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic __consumer_offsets \
  --formatter "kafka.coordinator.group.GroupMetadataManager\$OffsetsMessageFormatter" \
  --from-beginning
```

### 3.3 Monitor Consumer Lag
```bash
docker exec -it kafka-schema-pipeline-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group schema-pipeline-group
```

## 4. Key Points to Verify

### 4.1 Offset Commit Behavior
- Manual commit configuration ensures processing before commit
- Check logs for "Committed offset" messages
- Verify offset progress matches processed messages

### 4.2 Duplicate Processing Prevention
- Messages should be processed exactly once
- Check logs for duplicate message IDs
- Monitor offset commits after processing

### 4.3 Recovery Scenarios
- Application should resume from last committed offset after restart
- No messages should be lost during failure
- No duplicate processing after recovery

### 4.4 Performance Impact
- Monitor consumer lag during normal operation
- Check lag after recovery scenarios
- Verify processing throughput matches expectations

## 5. Common Issues and Resolution

### 5.1 Duplicate Processing
- Check manual commit configuration
- Verify transaction boundaries
- Monitor offset commit patterns

### 5.2 Message Loss
- Check auto.offset.reset configuration
- Verify commit synchronization
- Monitor partition assignments

### 5.3 Rebalancing Issues
- Check session timeout configuration
- Monitor heartbeat intervals
- Verify partition assignment strategy
