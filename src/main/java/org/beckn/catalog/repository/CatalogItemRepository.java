package org.beckn.catalog.repository;

import org.beckn.catalog.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CatalogItem entity operations
 */
@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, String> {

    /**
     * Find all items by provider ID
     */
    List<CatalogItem> findByProviderId(String providerId);

    /**
     * Find items by provider ID with pagination
     */
    List<CatalogItem> findByProviderIdOrderByCreatedAtDesc(String providerId);

    /**
     * Check if item exists by item ID and provider ID
     */
    boolean existsByItemIdAndProviderId(String itemId, String providerId);

    /**
     * Find items created after a specific date
     */
    List<CatalogItem> findByCreatedAtAfter(OffsetDateTime createdAfter);

    /**
     * Find items by item name containing text (case-insensitive)
     */
    @Query("SELECT c FROM CatalogItem c WHERE LOWER(c.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))")
    List<CatalogItem> findByItemNameContainingIgnoreCase(@Param("itemName") String itemName);

    /**
     * Count items by provider ID
     */
    long countByProviderId(String providerId);

    /**
     * Delete items by provider ID
     */
    void deleteByProviderId(String providerId);

    /**
     * Find items by multiple provider IDs
     */
    List<CatalogItem> findByProviderIdIn(List<String> providerIds);

    /**
     * Custom query to find items using JSONB data
     * Example: Find items with specific price currency
     */
    @Query(value = "SELECT * FROM catalog_items WHERE item_data->>'price'->>'currency' = :currency", 
           nativeQuery = true)
    List<CatalogItem> findItemsByCurrency(@Param("currency") String currency);

    /**
     * Custom query to find items within a price range using JSONB
     */
    @Query(value = "SELECT * FROM catalog_items WHERE " +
                   "CAST(item_data->'price'->>'value' AS DECIMAL) BETWEEN :minPrice AND :maxPrice", 
           nativeQuery = true)
    List<CatalogItem> findItemsByPriceRange(@Param("minPrice") Double minPrice, 
                                           @Param("maxPrice") Double maxPrice);
}
