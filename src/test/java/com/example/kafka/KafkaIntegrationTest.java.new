package com.example.kafka;

import com.example.kafka.messaging.producer.EventProducer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Collections;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(topics = {"events.input", "events.output", "events.dlt"}, partitions = 1)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "topics.input=events.input",
    "topics.output=events.output", 
    "topics.dlt=events.dlt"
})
class KafkaIntegrationTest {
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private EventProducer producer;

    @Value("${topics.input}")
    private String inputTopic;

    @Value("${topics.output}")
    private String outputTopic;

    @Value("${topics.dlt}")
    private String dltTopic;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        String groupId = "test-group-" + UUID.randomUUID();
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(groupId, "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps)
            .createConsumer(UUID.randomUUID().toString());
            
        // Clear any existing messages in topics
        consumer.subscribe(java.util.Arrays.asList(inputTopic, outputTopic, dltTopic));
        consumer.poll(Duration.ofMillis(500));
        consumer.commitSync();
        consumer.unsubscribe();
    }

    @Test
    void whenValidMessage_thenProcessedToOutputTopic() {
        // Given
        String eventId = UUID.randomUUID().toString();
        String validMessage = String.format("""
            {
                "id": "%s",
                "ts": "2025-09-02T10:00:00Z",
                "type": "CREATE",
                "payload": {
                    "name": "test"
                }
            }
            """, eventId);

        // When
        producer.sendToOutput("test-key", validMessage);
        consumer.subscribe(Collections.singleton(outputTopic));

        // Then
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, outputTopic);
            assertThat(record).isNotNull();
            assertThat(record.value()).contains(eventId);
        });
    }

    @Test
    void whenInvalidMessage_thenRoutedToDlt() {
        // Given an invalid message with incorrect timestamp format
        String invalidMessage = """
            {
                "id": "123e4567-e89b-12d3-a456-426614174000",
                "ts": "not-a-timestamp",
                "type": "CREATE",
                "payload": {}
            }
            """;
            
        // Subscribe to DLT topic and ensure we start from the beginning
        consumer.subscribe(Collections.singleton(dltTopic));
        consumer.poll(Duration.ofMillis(500));
        consumer.commitSync();
        consumer.seekToBeginning(consumer.assignment());
        
        // When - Send the invalid message
        producer.send("test-key", invalidMessage, inputTopic);
        
        // Then verify it gets routed to DLT
        await()
            .pollInterval(Duration.ofMillis(500))
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, String> record : records) {
                    if (record.value().equals(invalidMessage)) {
                        assertThat(record.headers()).isNotNull();
                        assertThat(record.headers().headers("x-error")).isNotEmpty();
                        
                        String errorMsg = new String(record.headers()
                            .headers("x-error")
                            .iterator()
                            .next()
                            .value());
                            
                        System.out.println("Found DLT message. Error: " + errorMsg);
                        return;
                    }
                }
                
                throw new AssertionError("Expected invalid message not found in DLT topic");
            });
    }
}
