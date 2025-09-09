package com.example.kafka.messaging.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Value("${kafka.producer.sync-timeout-ms}")
    private long syncTimeoutMs;

    @Value("${topics.output}")
    private String outputTopic;

    @Value("${topics.dlt}")
    private String dltTopic;

    public void sendToOutput(String key, String value) {
        if (key == null) {
            // If no key is provided, generate one to distribute across partitions
            key = String.valueOf(System.nanoTime() % 3); // This will generate keys 0, 1, or 2
        }
        send(key, value, outputTopic);
    }

    public void sendToDlt(String key, String value, String originalTopic, 
                         int originalPartition, long originalOffset, 
                         String errorMessage, String errorClass) {
        ProducerRecord<String, String> record = new ProducerRecord<>(dltTopic, key, value);
        
        // Add error metadata headers
        record.headers()
            .add(new RecordHeader("x-error", errorMessage.getBytes(StandardCharsets.UTF_8)))
            .add(new RecordHeader("x-error-class", errorClass.getBytes(StandardCharsets.UTF_8)))
            .add(new RecordHeader("x-original-topic", originalTopic.getBytes(StandardCharsets.UTF_8)))
            .add(new RecordHeader("x-original-partition", String.valueOf(originalPartition).getBytes(StandardCharsets.UTF_8)))
            .add(new RecordHeader("x-original-offset", String.valueOf(originalOffset).getBytes(StandardCharsets.UTF_8)));

        send(record);
    }

    public void send(String key, String value, String topic) {
        send(new ProducerRecord<>(topic, key, value));
    }

    private void send(ProducerRecord<String, String> record) {
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
            future.get(syncTimeoutMs, TimeUnit.MILLISECONDS);
            log.debug("Message sent successfully to topic: {}", record.topic());
        } catch (Exception e) {
            log.error("Failed to send message to topic: {}", record.topic(), e);
            throw new RuntimeException("Failed to send message", e);
        }
    }
}
