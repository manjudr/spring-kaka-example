# Example Kafka Events

## 1. Valid Events

### 1.1 Basic Valid Event
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

### 1.2 Valid Event with Complex Payload
```json
{
  "context": {
    "domain": "mobility",
    "action": "on_search",
    "timestamp": "2023-09-02T10:30:00Z"
  },
  "type": "ORDER",
  "payload": {
    "order_id": "123456",
    "items": [
      {
        "id": "item1",
        "quantity": 2,
        "price": 100.50
      }
    ],
    "delivery": {
      "address": "123 Main St",
      "pincode": "560001"
    }
  }
}
```

### 1.3 Valid Event with Minimal Fields
```json
{
  "context": {
    "domain": "logistics",
    "action": "track",
    "timestamp": "2023-09-02T11:00:00Z"
  },
  "type": "STATUS",
  "payload": {}
}
```

## 2. Invalid Events

### 2.1 Missing Required Fields
```json
{
  "context": {
    "action": "search"
  },
  "type": "NOTIFICATION",
  "payload": {
    "query": "test"
  }
}
```

### 2.2 Invalid Timestamp Format
```json
{
  "context": {
    "domain": "retail",
    "action": "search",
    "timestamp": "2023-09-02"
  },
  "type": "NOTIFICATION",
  "payload": {
    "query": "test"
  }
}
```

### 2.3 Invalid Event Type
```json
{
  "context": {
    "domain": "retail",
    "action": "search",
    "timestamp": "2023-09-02T10:00:00Z"
  },
  "type": "INVALID_TYPE",
  "payload": {
    "query": "test"
  }
}
```

### 2.4 Invalid JSON Format
```json
{
  "context": {
    "domain": "retail"
    "action": "search",  // Missing comma
    "timestamp": "2023-09-02T10:00:00Z"
  },
  "type": "NOTIFICATION",
  "payload": {
    "query": "test"
  }
}
```

### 2.5 Invalid Data Types
```json
{
  "context": {
    "domain": 123,  // Should be string
    "action": "search",
    "timestamp": "2023-09-02T10:00:00Z"
  },
  "type": "NOTIFICATION",
  "payload": {
    "query": ["test"]  // Should be string
  }
}
```

## 3. How to Test

### 3.1 Start Producer
```bash
docker exec -it kafka-schema-pipeline-kafka-1 kafka-console-producer --bootstrap-server localhost:9092 --topic events.input
```

### 3.2 Monitor Application Logs
```bash
docker compose logs -f app
```

### 3.3 Check Consumer Output
```bash
docker exec -it kafka-schema-pipeline-kafka-1 kafka-console-consumer --bootstrap-server localhost:9092 --topic events.input --from-beginning
```

### 3.4 Testing Steps:
1. Copy a valid event
2. Paste it into the producer console
3. Check application logs for validation result
4. Check consumer output to verify message delivery
5. Repeat with invalid events to verify rejection

### 3.5 Expected Behavior:
- Valid events: Will be processed and acknowledged
- Invalid events: Will be rejected with validation error in logs

## 4. Additional Notes

### 4.1 Timestamp Formats
- Valid format: ISO-8601 (e.g., "2023-09-02T10:00:00Z")
- Invalid formats:
  - "2023-09-02"
  - "2023/09/02 10:00:00"
  - "02-09-2023"

### 4.2 Event Types
Valid types:
- NOTIFICATION
- ORDER
- STATUS

### 4.3 Required Fields
- context.domain
- context.action
- context.timestamp
- type
- payload (can be empty object)
