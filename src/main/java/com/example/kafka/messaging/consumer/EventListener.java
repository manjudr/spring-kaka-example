package com.example.kafka.messaging.consumer;

import com.example.kafka.model.Event;
import com.example.kafka.service.EventValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.example.kafka.messaging.producer.EventProducer;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventListener {
    private final EventValidator validator;
    private final EventProducer producer;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.listener.concurrency:1}")
    private String configuredConcurrency;

    @KafkaListener(
        topics = "${topics.input}",
        containerFactory = "kafkaListenerContainerFactory",
        concurrency = "${spring.kafka.listener.concurrency:1}"
    )
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Using configured concurrency: {}", configuredConcurrency);
        String value = record.value();
        log.info("Received message: {}", value);
        
        try {
            // Validate against schema
            validator.validate(value);
            
            // Parse to check if we can deserialize
            Event event = objectMapper.readValue(value, Event.class);
            
            // Send to output topic
            producer.sendToOutput(record.key(), value);
            
            // Commit offset only after successful processing
            ack.acknowledge();
            log.info("Successfully processed message: {}", event.getId());
            
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
            
            // Send to DLT with error metadata
            producer.sendToDlt(
                record.key(),
                value,
                record.topic(),
                record.partition(),
                record.offset(),
                e.getMessage(),
                e.getClass().getName()
            );
            
            // Still acknowledge since we handled the error
            ack.acknowledge();
        }
    }
}
