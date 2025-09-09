package org.beckn.catalog.service;

import org.beckn.catalog.entity.CatalogItem;
import org.beckn.catalog.repository.CatalogItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for processing Beckn catalog events and managing catalog items
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogItemService {

    private final CatalogItemRepository catalogItemRepository;
    private final ObjectMapper objectMapper;

    /**
     * Process a Beckn catalog event and extract/store catalog items
     * 
     * @param becknEventJson Raw Beckn catalog event JSON
     * @return List of stored CatalogItem entities
     */
    @Transactional
    public List<CatalogItem> processBecknCatalogEvent(String becknEventJson) {
        log.info("Processing Beckn catalog event");
        
        try {
            JsonNode rootNode = objectMapper.readTree(becknEventJson);
            JsonNode catalogNode = rootNode.path("message").path("catalog");
            JsonNode providersNode = catalogNode.path("providers");
            
            if (!providersNode.isArray()) {
                log.warn("No providers found in Beckn catalog event");
                return new ArrayList<>();
            }
            
            List<CatalogItem> savedItems = new ArrayList<>();
            
            // Process each provider
            for (JsonNode providerNode : providersNode) {
                String providerId = providerNode.path("id").asText();
                
                if (providerId.isEmpty()) {
                    log.warn("Skipping provider with missing ID");
                    continue;
                }
                
                log.info("Processing provider: {}", providerId);
                
                // Process items for this provider
                JsonNode itemsNode = providerNode.path("items");
                if (itemsNode.isArray()) {
                    for (JsonNode itemNode : itemsNode) {
                        try {
                            CatalogItem catalogItem = extractCatalogItem(providerId, itemNode);
                            if (catalogItem != null) {
                                CatalogItem savedItem = saveOrUpdateCatalogItem(catalogItem);
                                savedItems.add(savedItem);
                            }
                        } catch (Exception e) {
                            log.error("Error processing item for provider {}: {}", providerId, e.getMessage(), e);
                            // Continue processing other items
                        }
                    }
                }
            }
            
            log.info("Successfully processed {} catalog items from Beckn event", savedItems.size());
            return savedItems;
            
        } catch (Exception e) {
            log.error("Error processing Beckn catalog event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process Beckn catalog event", e);
        }
    }

    /**
     * Extract CatalogItem from item JSON node
     */
    private CatalogItem extractCatalogItem(String providerId, JsonNode itemNode) {
        try {
            String itemId = itemNode.path("id").asText();
            
            if (itemId.isEmpty()) {
                log.warn("Skipping item with missing ID for provider: {}", providerId);
                return null;
            }
            
            // Extract item name from descriptor
            String itemName = itemNode.path("descriptor").path("name").asText();
            if (itemName.isEmpty()) {
                itemName = null; // Allow null item names
            }
            
            // Create CatalogItem with raw JSON data
            CatalogItem catalogItem = new CatalogItem();
            catalogItem.setItemId(itemId);
            catalogItem.setItemName(itemName);
            catalogItem.setProviderId(providerId);
            catalogItem.setItemData(itemNode); // Store complete raw item JSON
            
            log.debug("Extracted catalog item: {} from provider: {}", itemId, providerId);
            return catalogItem;
            
        } catch (Exception e) {
            log.error("Error extracting catalog item: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Save or update catalog item (upsert operation)
     */
    @Transactional
    public CatalogItem saveOrUpdateCatalogItem(CatalogItem catalogItem) {
        try {
            // Check if item already exists
            Optional<CatalogItem> existingItem = catalogItemRepository.findById(catalogItem.getItemId());
            
            if (existingItem.isPresent()) {
                // Update existing item
                CatalogItem existing = existingItem.get();
                existing.setItemName(catalogItem.getItemName());
                existing.setProviderId(catalogItem.getProviderId());
                existing.setItemData(catalogItem.getItemData());
                existing.setUpdatedBy("system");
                
                log.debug("Updating existing catalog item: {}", catalogItem.getItemId());
                return catalogItemRepository.save(existing);
            } else {
                // Create new item
                log.debug("Creating new catalog item: {}", catalogItem.getItemId());
                return catalogItemRepository.save(catalogItem);
            }
            
        } catch (Exception e) {
            log.error("Error saving catalog item {}: {}", catalogItem.getItemId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save catalog item: " + catalogItem.getItemId(), e);
        }
    }

    /**
     * Find catalog item by ID
     */
    public Optional<CatalogItem> findById(String itemId) {
        return catalogItemRepository.findById(itemId);
    }

    /**
     * Find all catalog items by provider ID
     */
    public List<CatalogItem> findByProviderId(String providerId) {
        return catalogItemRepository.findByProviderId(providerId);
    }

    /**
     * Delete catalog item by ID
     */
    @Transactional
    public void deleteById(String itemId) {
        catalogItemRepository.deleteById(itemId);
        log.info("Deleted catalog item: {}", itemId);
    }

    /**
     * Delete all catalog items for a provider
     */
    @Transactional
    public void deleteByProviderId(String providerId) {
        catalogItemRepository.deleteByProviderId(providerId);
        log.info("Deleted all catalog items for provider: {}", providerId);
    }

    /**
     * Get count of items by provider
     */
    public long getItemCountByProvider(String providerId) {
        return catalogItemRepository.countByProviderId(providerId);
    }
}
