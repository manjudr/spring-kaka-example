# Kafka Schema Pipeline

A Spring Boot application that demonstrates Kafka message processing with JSON Schema validation.

## Features

- Consumes JSON events from Kafka input topic
- Validates events against JSON Schema
- Routes valid events to output topic
- Routes invalid events to DLT with error metadata
- Manual offset control with acknowledgments
- Robust error handling with retries

## Prerequisites

- Java 17
- Maven
- Docker and Docker Compose

## Build and Run

### Building the Application

```bash
mvn clean verify
```

### Running with Docker Compose

```bash
docker compose up --build
```

This will start:
- Zookeeper
- Kafka (3.6.0)
- The Spring Boot application

### Testing the Pipeline

1. Produce a valid message:
```bash
kafka-console-producer.sh --bootstrap-server localhost:29092 --topic events.input
```

Example valid message:
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "ts": "2025-09-02T10:00:00Z",
  "type": "CREATE",
  "payload": {
    "name": "test"
  }
}
```

2. Check the output topic:
```bash
kafka-console-consumer.sh --bootstrap-server localhost:29092 --topic events.output --from-beginning
```

3. Check the DLT for invalid messages:
```bash
kafka-console-consumer.sh --bootstrap-server localhost:29092 --topic events.dlt --from-beginning
```

## Configuration

Key configurations in `application.yml`:

```yaml
topics:
  input: events.input
  output: events.output
  dlt: events.dlt

kafka:
  topics:
    partitions: 3
    replication-factor: 1
  listener:
    concurrency: 3
```

### Changing Topic Names

Modify the corresponding properties in `application.yml`:

```yaml
topics:
  input: your.input.topic
  output: your.output.topic
  dlt: your.dlt.topic
```

### Adjusting Partitions/Replication

Modify the kafka.topics section in `application.yml`:

```yaml
kafka:
  topics:
    partitions: 6           # Number of partitions
    replication-factor: 3   # Replication factor
```

## Testing

- Unit tests: `mvn test`
- Integration tests: `mvn verify`

## Monitoring

The application exposes the following actuator endpoints:
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics
- Info: http://localhost:8080/actuator/info

## Error Handling

- Schema validation errors are sent to DLT with headers:
  - x-error: Error message
  - x-error-class: Exception class name
  - x-original-topic: Original topic
  - x-original-partition: Original partition
  - x-original-offset: Original offset

- Processing errors are retried with exponential backoff before being sent to DLT
