package org.beckn.catalog.messaging.consumer;

import org.beckn.catalog.entity.CatalogItem;
import org.beckn.catalog.messaging.producer.CatalogEventProducer;
import org.beckn.catalog.service.CatalogItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.beckn.catalog.messaging.producer.EventProducer;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventListener {
    private final EventProducer producer;
    private final CatalogItemService catalogItemService;
    private final CatalogEventProducer catalogEventProducer;
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
        log.info("Received native Beckn catalog event for processing");
        
        String providerId = null;
        try {
            // Validate that this is a Beckn catalog event
            if (!isValidBecknCatalogEvent(value)) {
                throw new IllegalArgumentException("Invalid Beckn catalog event format - missing required fields");
            }
            
            // Process Beckn catalog event and store items to PostgreSQL
            List<CatalogItem> storedItems = catalogItemService.processBecknCatalogEvent(value);
            
            if (!storedItems.isEmpty()) {
                // Extract provider ID from the first item
                providerId = storedItems.get(0).getProviderId();

                // Publish success event with full item data
                catalogEventProducer.publishItemsStoredEvent(providerId, storedItems);
                
                log.info("Successfully processed and stored {} items for provider: {}", 
                        storedItems.size(), providerId);
            } else {
                log.warn("No items were extracted from Beckn catalog event");
            }
            
            // Commit offset only after successful processing
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing native Beckn catalog event: {}", e.getMessage(), e);
            
            // Try to extract provider ID for error event
            if (providerId == null) {
                try {
                    providerId = extractProviderIdFromEvent(value);
                } catch (Exception ex) {
                    providerId = "unknown";
                }
            }
            
            // Publish error event
            catalogEventProducer.publishCatalogProcessingErrorEvent(providerId, e.getMessage(), value);
            
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

    /**
     * Validate that this is a proper Beckn catalog event with required structure
     */
    private boolean isValidBecknCatalogEvent(String eventJson) {
        try {
            var rootNode = objectMapper.readTree(eventJson);
            
            // Check for required top-level fields
            if (!rootNode.has("context") || !rootNode.has("message")) {
                log.debug("Missing required top-level fields: context or message");
                return false;
            }
            
            // Check context structure
            var contextNode = rootNode.path("context");
            if (!contextNode.has("domain") || !contextNode.has("action")) {
                log.debug("Missing required context fields: domain or action");
                return false;
            }
            
            // Check message structure for catalog events
            var messageNode = rootNode.path("message");
            if (!messageNode.has("catalog")) {
                log.debug("Missing catalog in message - not a catalog event");
                return false;
            }
            
            // Check if catalog has providers
            var catalogNode = messageNode.path("catalog");
            if (!catalogNode.has("providers")) {
                log.debug("Missing providers in catalog");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.debug("Failed to parse JSON for validation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract provider ID from Beckn event for error reporting
     */
    private String extractProviderIdFromEvent(String eventJson) {
        try {
            var rootNode = objectMapper.readTree(eventJson);
            var providersNode = rootNode.path("message").path("catalog").path("providers");
            
            if (providersNode.isArray() && providersNode.size() > 0) {
                return providersNode.get(0).path("id").asText("unknown");
            }
            return "unknown";
        } catch (Exception e) {
            log.debug("Could not extract provider ID from event: {}", e.getMessage());
            return "unknown";
        }
    }
}
