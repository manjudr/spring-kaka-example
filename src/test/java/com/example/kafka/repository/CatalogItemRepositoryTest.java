package com.example.kafka.repository;

import com.example.kafka.entity.CatalogItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogItemRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private CatalogItemRepository catalogItemRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSaveAndRetrieveCatalogItem() throws Exception {
        // Given
        String itemData = """
            {
                "id": "test-item-001",
                "descriptor": {
                    "name": "Test Item"
                },
                "price": {
                    "currency": "USD",
                    "value": "99.99"
                }
            }
            """;
        
        JsonNode itemDataNode = objectMapper.readTree(itemData);
        
        CatalogItem catalogItem = new CatalogItem();
        catalogItem.setItemId("test-item-001");
        catalogItem.setItemName("Test Item");
        catalogItem.setProviderId("test-provider-001");
        catalogItem.setItemData(itemDataNode);

        // When
        CatalogItem savedItem = catalogItemRepository.save(catalogItem);

        // Then
        assertThat(savedItem.getItemId()).isEqualTo("test-item-001");
        assertThat(savedItem.getItemName()).isEqualTo("Test Item");
        assertThat(savedItem.getProviderId()).isEqualTo("test-provider-001");
        assertThat(savedItem.getItemData()).isNotNull();
        assertThat(savedItem.getCreatedAt()).isNotNull();
        assertThat(savedItem.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindByProviderId() throws Exception {
        // Given
        createTestItem("item1", "Test Item 1", "provider1");
        createTestItem("item2", "Test Item 2", "provider1");
        createTestItem("item3", "Test Item 3", "provider2");

        // When
        List<CatalogItem> provider1Items = catalogItemRepository.findByProviderId("provider1");
        List<CatalogItem> provider2Items = catalogItemRepository.findByProviderId("provider2");

        // Then
        assertThat(provider1Items).hasSize(2);
        assertThat(provider2Items).hasSize(1);
        assertThat(provider1Items).extracting(CatalogItem::getProviderId).containsOnly("provider1");
    }

    @Test
    void shouldCheckIfItemExists() throws Exception {
        // Given
        createTestItem("item1", "Test Item 1", "provider1");

        // When & Then
        assertThat(catalogItemRepository.existsByItemIdAndProviderId("item1", "provider1")).isTrue();
        assertThat(catalogItemRepository.existsByItemIdAndProviderId("item1", "provider2")).isFalse();
        assertThat(catalogItemRepository.existsByItemIdAndProviderId("item2", "provider1")).isFalse();
    }

    @Test
    void shouldCountItemsByProvider() throws Exception {
        // Given
        createTestItem("item1", "Test Item 1", "provider1");
        createTestItem("item2", "Test Item 2", "provider1");
        createTestItem("item3", "Test Item 3", "provider2");

        // When & Then
        assertThat(catalogItemRepository.countByProviderId("provider1")).isEqualTo(2);
        assertThat(catalogItemRepository.countByProviderId("provider2")).isEqualTo(1);
        assertThat(catalogItemRepository.countByProviderId("provider3")).isEqualTo(0);
    }

    @Test
    void shouldFindByItemNameContaining() throws Exception {
        // Given
        createTestItem("item1", "iPhone 15 Pro", "provider1");
        createTestItem("item2", "Samsung Galaxy", "provider1");
        createTestItem("item3", "iPhone 14", "provider2");

        // When
        List<CatalogItem> iphoneItems = catalogItemRepository.findByItemNameContainingIgnoreCase("iphone");
        List<CatalogItem> galaxyItems = catalogItemRepository.findByItemNameContainingIgnoreCase("galaxy");

        // Then
        assertThat(iphoneItems).hasSize(2);
        assertThat(galaxyItems).hasSize(1);
        assertThat(iphoneItems).extracting(CatalogItem::getItemName)
                .contains("iPhone 15 Pro", "iPhone 14");
    }

    @Test
    void shouldUpdateExistingItem() throws Exception {
        // Given
        CatalogItem originalItem = createTestItem("item1", "Original Name", "provider1");
        
        // When
        originalItem.setItemName("Updated Name");
        CatalogItem updatedItem = catalogItemRepository.save(originalItem);

        // Then
        Optional<CatalogItem> found = catalogItemRepository.findById("item1");
        assertThat(found).isPresent();
        assertThat(found.get().getItemName()).isEqualTo("Updated Name");
        assertThat(found.get().getUpdatedAt()).isAfter(found.get().getCreatedAt());
    }

    @Test
    void shouldDeleteByProviderId() throws Exception {
        // Given
        createTestItem("item1", "Test Item 1", "provider1");
        createTestItem("item2", "Test Item 2", "provider1");
        createTestItem("item3", "Test Item 3", "provider2");

        // When
        catalogItemRepository.deleteByProviderId("provider1");

        // Then
        assertThat(catalogItemRepository.findByProviderId("provider1")).isEmpty();
        assertThat(catalogItemRepository.findByProviderId("provider2")).hasSize(1);
    }

    private CatalogItem createTestItem(String itemId, String itemName, String providerId) throws Exception {
        String itemData = String.format("""
            {
                "id": "%s",
                "descriptor": {
                    "name": "%s"
                },
                "price": {
                    "currency": "USD",
                    "value": "99.99"
                }
            }
            """, itemId, itemName);
        
        JsonNode itemDataNode = objectMapper.readTree(itemData);
        
        CatalogItem catalogItem = new CatalogItem();
        catalogItem.setItemId(itemId);
        catalogItem.setItemName(itemName);
        catalogItem.setProviderId(providerId);
        catalogItem.setItemData(itemDataNode);

        return catalogItemRepository.save(catalogItem);
    }
}
