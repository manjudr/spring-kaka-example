package org.beckn.catalog.messaging.producer;

import org.beckn.catalog.entity.CatalogItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka producer for publishing catalog-related events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${topics.output}")
    private String outputTopic;

    /**
     * Publish individual item events when catalog items are successfully stored to PostgreSQL
     * Each item gets its own event for downstream Elasticsearch processing
     */
    public void publishItemsStoredEvent(String providerId, List<CatalogItem> storedItems) {
        try {
            log.info("Publishing {} individual item events to topic: {} for provider: {}", 
                    storedItems.size(), outputTopic, providerId);
            
            // Publish each item as a separate event
            for (CatalogItem item : storedItems) {
                publishSingleItemEvent(item);
            }
            
            log.info("Successfully published {} individual item events for provider: {}", 
                    storedItems.size(), providerId);
                    
        } catch (Exception e) {
            log.error("Error publishing item events for provider {}: {}", providerId, e.getMessage(), e);
        }
    }
    
    /**
     * Publish event for single item storage
     */
    public void publishItemStoredEvent(String providerId, CatalogItem item) {
        publishItemsStoredEvent(providerId, List.of(item));
    }
    
    /**
     * Publish a single item event - original Beckn format with metadata
     */
    private void publishSingleItemEvent(CatalogItem item) {
        try {
            Map<String, Object> event = createSingleItemEvent(item);
            String eventJson = objectMapper.writeValueAsString(event);
            
            kafkaTemplate.send(outputTopic, item.getItemId(), eventJson)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Successfully published item event for: {}", item.getItemId());
                    } else {
                        log.error("Failed to publish item event for {}: {}", 
                                item.getItemId(), ex.getMessage(), ex);
                    }
                });
                
        } catch (Exception e) {
            log.error("Error publishing single item event for {}: {}", item.getItemId(), e.getMessage(), e);
        }
    }
    
    /**
     * Create single item event with original Beckn format + metadata
     */
    private Map<String, Object> createSingleItemEvent(CatalogItem item) {
        Map<String, Object> event = new HashMap<>();
        
        // Event metadata
        event.put("event_type", "catalog_item_stored");
        event.put("event_id", java.util.UUID.randomUUID().toString());
        event.put("timestamp", OffsetDateTime.now().toString());
        event.put("source", "catalog-publish");
        event.put("version", "2.0");
        
        // Item metadata
        event.put("item_id", item.getItemId());
        event.put("provider_id", item.getProviderId());
        event.put("created_at", item.getCreatedAt().toString());
        event.put("updated_at", item.getUpdatedAt().toString());
        
        // Original Beckn item data (let downstream job handle transformation)
        event.put("item_data", item.getItemData());
        
        return event;
    }

    /**
     * Publish error event when catalog processing fails
     */
    public void publishCatalogProcessingErrorEvent(String providerId, String errorMessage, String originalEvent) {
        try {
            Map<String, Object> event = createErrorEvent(providerId, errorMessage, originalEvent);
            String eventJson = objectMapper.writeValueAsString(event);
            
            kafkaTemplate.send(outputTopic, providerId, eventJson)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published catalog processing error event for provider: {}", providerId);
                    } else {
                        log.error("Failed to publish catalog processing error event for provider: {}: {}", 
                                providerId, ex.getMessage(), ex);
                    }
                });
                
        } catch (Exception e) {
            log.error("Error publishing catalog processing error event for provider {}: {}", 
                    providerId, e.getMessage(), e);
        }
    }

    /**
     * Create error event payload
     */
    private Map<String, Object> createErrorEvent(String providerId, String errorMessage, String originalEvent) {
        Map<String, Object> event = new HashMap<>();
        event.put("event_type", "catalog_processing_error");
        event.put("event_id", java.util.UUID.randomUUID().toString());
        event.put("timestamp", OffsetDateTime.now().toString());
        event.put("provider_id", providerId);
        event.put("error_message", errorMessage);
        event.put("original_event", originalEvent);
        event.put("source", "catalog-publish");
        event.put("version", "2.0");
        
        return event;
    }
}