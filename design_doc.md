# Elasticsearch Index Design for Catalog Search

## Overview

This document outlines the design strategies for creating an Elasticsearch index mapping for Beckn catalog data, focusing on efficient search capabilities while maintaining data integrity and performance.

## Design Strategy Analysis

### 1. Document Structure Strategy

**Approach: Item-Centric Document Model**

Each item becomes a separate document in the index, with provider and catalog context embedded. This approach provides:

**Pros:**
- Efficient item-level search and filtering
- Better performance for item-specific queries
- Easier aggregation and faceting
- Simplified query construction
- Better scalability for large catalogs

**Cons:**
- Data duplication (provider info repeated for each item)
- Larger index size
- More complex updates when provider data changes

### 2. Nested Field Handling Strategy

**Approach: Hybrid Flattening with Nested Objects**

For complex nested structures, use a combination of:
- Flattened fields for simple searches (using `_` separator)
- Nested objects for complex relationships that need to maintain context

**Field Flattening Examples:**
- `provider_descriptor_name` → "Best Buy"
- `provider_locations_gps` → ["44.9778,-93.2650", "40.7589,-73.9851"]
- `item_tags_features_5g` → "Y"

**Nested Objects for:**
- Tags with descriptor-value relationships
- Offer-item relationships
- Location details with GPS coordinates

### 3. Tags Structure Strategy

**Approach: Normalized Tag Storage**

Store tags in a normalized format to enable efficient filtering:

```json
{
  "tags": [
    {
      "category": "features",
      "code": "5g",
      "value": "Y",
      "display": true
    }
  ]
}
```

This allows for efficient queries like:
- `tags.category: "features" AND tags.code: "5g" AND tags.value: "Y"`

### 4. Offer-Item Relationship Strategy

**Approach: Embedded Offer Information**

Store offer details directly in item documents with:
- `applicable_offers`: Array of offer IDs that apply to this item
- `offer_details`: Embedded offer information for quick access

## Elasticsearch Index Mapping

### Option 1: Flattened Structure (Recommended)

```json
{
  "mappings": {
    "properties": {
      "item_id": {
        "type": "keyword"
      },
      "provider_id": {
        "type": "keyword"
      },
      "provider_descriptor_name": {
        "type": "text",
        "fields": {
          "keyword": {
            "type": "keyword"
          }
        }
      },
      "provider_descriptor_code": {
        "type": "keyword"
      },
      "provider_locations": {
        "type": "nested",
        "properties": {
          "id": {"type": "keyword"},
          "gps": {"type": "geo_point"},
          "address": {"type": "text"},
          "city_name": {"type": "keyword"},
          "state_name": {"type": "keyword"},
          "country_name": {"type": "keyword"},
          "area_code": {"type": "keyword"}
        }
      },
      "provider_fulfillments": {
        "type": "nested",
        "properties": {
          "id": {"type": "keyword"},
          "type": {"type": "keyword"},
          "rateable": {"type": "boolean"},
          "tracking": {"type": "boolean"},
          "state_code": {"type": "keyword"},
          "contact_phone": {"type": "keyword"},
          "contact_email": {"type": "keyword"}
        }
      },
      "provider_categories": {
        "type": "nested",
        "properties": {
          "id": {"type": "keyword"},
          "code": {"type": "keyword"},
          "name": {"type": "text"}
        }
      },
      "applicable_offers": {
        "type": "keyword"
      },
      "offer_details": {
        "type": "nested",
        "properties": {
          "id": {"type": "keyword"},
          "name": {"type": "text"},
          "short_desc": {"type": "text"},
          "offer_type": {"type": "keyword"},
          "start_date": {"type": "date"},
          "end_date": {"type": "date"}
        }
      },
      "item_category_ids": {
        "type": "keyword"
      },
      "item_fulfillment_id": {
        "type": "keyword"
      },
      "item_location_id": {
        "type": "keyword"
      },
      "item_descriptor_name": {
        "type": "text",
        "fields": {
          "keyword": {"type": "keyword"},
          "suggest": {
            "type": "completion"
          }
        }
      },
      "item_descriptor_long_desc": {
        "type": "text"
      },
      "item_images": {
        "type": "nested",
        "properties": {
          "url": {"type": "keyword"},
          "size_type": {"type": "keyword"},
          "width": {"type": "integer"},
          "height": {"type": "integer"}
        }
      },
      "price_currency": {
        "type": "keyword"
      },
      "price_value": {
        "type": "float"
      },
      "price_listed_value": {
        "type": "float"
      },
      "matched": {
        "type": "boolean"
      },
      "rating": {
        "type": "float"
      },
      "tags": {
        "type": "nested",
        "properties": {
          "category": {"type": "keyword"},
          "code": {"type": "keyword"},
          "value": {"type": "keyword"},
          "display": {"type": "boolean"}
        }
      },
      "return_terms": {
        "type": "nested",
        "properties": {
          "fulfillment_state_code": {"type": "keyword"},
          "return_eligible": {"type": "boolean"},
          "return_start_date": {"type": "date"},
          "return_end_date": {"type": "date"},
          "fulfillment_managed_by": {"type": "keyword"}
        }
      },
      "refund_terms": {
        "type": "nested",
        "properties": {
          "fulfillment_state_code": {"type": "keyword"},
          "refund_eligible": {"type": "boolean"},
          "refund_start_date": {"type": "date"},
          "refund_end_date": {"type": "date"},
          "refund_amount": {"type": "float"}
        }
      },
      "replacement_terms": {
        "type": "nested",
        "properties": {
          "fulfillment_state_code": {"type": "keyword"},
          "replace_start_date": {"type": "date"},
          "replace_end_date": {"type": "date"},
          "external_ref_url": {"type": "keyword"}
        }
      },
      "raw_item_json": {
        "type": "object",
        "enabled": false
      },
      "created_at": {
        "type": "date"
      },
      "updated_at": {
        "type": "date"
      }
    }
  },
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "catalog_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "stop", "snowball"]
        }
      }
    }
  }
}
```

### Option 2: Nested Structure (Alternative)

```json
{
  "mappings": {
    "properties": {
      "item": {
        "type": "object",
        "properties": {
          "id": {"type": "keyword"},
          "category_ids": {"type": "keyword"},
          "fulfillment_id": {"type": "keyword"},
          "location_id": {"type": "keyword"},
          "descriptor": {
            "type": "object",
            "properties": {
              "name": {"type": "text"},
              "long_desc": {"type": "text"}
            }
          },
          "price": {
            "type": "object",
            "properties": {
              "currency": {"type": "keyword"},
              "value": {"type": "float"},
              "listed_value": {"type": "float"}
            }
          },
          "tags": {
            "type": "nested",
            "properties": {
              "descriptor": {
                "type": "object",
                "properties": {
                  "name": {"type": "keyword"},
                  "code": {"type": "keyword"}
                }
              },
              "list": {
                "type": "nested",
                "properties": {
                  "descriptor": {
                    "type": "object",
                    "properties": {
                      "name": {"type": "keyword"},
                      "code": {"type": "keyword"}
                    }
                  },
                  "value": {"type": "keyword"}
                }
              }
            }
          }
        }
      },
      "provider": {
        "type": "object",
        "properties": {
          "id": {"type": "keyword"},
          "descriptor": {
            "type": "object",
            "properties": {
              "name": {"type": "text"},
              "code": {"type": "keyword"}
            }
          },
          "locations": {
            "type": "nested",
            "properties": {
              "id": {"type": "keyword"},
              "gps": {"type": "geo_point"},
              "address": {"type": "text"},
              "city": {
                "type": "object",
                "properties": {
                  "name": {"type": "keyword"}
                }
              },
              "state": {
                "type": "object",
                "properties": {
                  "name": {"type": "keyword"}
                }
              },
              "country": {
                "type": "object",
                "properties": {
                  "name": {"type": "keyword"}
                }
              }
            }
          },
          "offers": {
            "type": "nested",
            "properties": {
              "id": {"type": "keyword"},
              "descriptor": {
                "type": "object",
                "properties": {
                  "name": {"type": "text"},
                  "short_desc": {"type": "text"}
                }
              },
              "item_ids": {"type": "keyword"},
              "time": {
                "type": "object",
                "properties": {
                  "range": {
                    "type": "object",
                    "properties": {
                      "start": {"type": "date"},
                      "end": {"type": "date"}
                    }
                  }
                }
              }
            }
          }
        }
      },
      "raw_item_json": {
        "type": "object",
        "enabled": false
      }
    }
  }
}
```

## Query Examples

### 1. Basic Item Search

```json
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "item_descriptor_name": "iPhone"
          }
        }
      ],
      "filter": [
        {
          "term": {
            "price_currency": "USD"
          }
        }
      ]
    }
  }
}
```

### 2. Tag-Based Filtering

```json
{
  "query": {
    "bool": {
      "must": [
        {
          "nested": {
            "path": "tags",
            "query": {
              "bool": {
                "must": [
                  {"term": {"tags.category": "features"}},
                  {"term": {"tags.code": "5g"}},
                  {"term": {"tags.value": "Y"}}
                ]
              }
            }
          }
        }
      ]
    }
  }
}
```

### 3. Location-Based Search

```json
{
  "query": {
    "bool": {
      "filter": [
        {
          "geo_distance": {
            "distance": "50km",
            "provider_locations.gps": {
              "lat": 40.7589,
              "lon": -73.9851
            }
          }
        }
      ]
    }
  }
}
```

### 4. Offer-Based Filtering

```json
{
  "query": {
    "bool": {
      "must": [
        {
          "nested": {
            "path": "offer_details",
            "query": {
              "bool": {
                "must": [
                  {"term": {"offer_details.offer_type": "holiday-sale"}},
                  {
                    "range": {
                      "offer_details.start_date": {
                        "lte": "now"
                      }
                    }
                  },
                  {
                    "range": {
                      "offer_details.end_date": {
                        "gte": "now"
                      }
                    }
                  }
                ]
              }
            }
          }
        }
      ]
    }
  }
}
```

## Performance Considerations

### 1. Index Settings

- **Shards**: 3 shards for optimal distribution
- **Replicas**: 1 replica for high availability
- **Refresh Interval**: 30s for better indexing performance
- **Translog**: 1s flush for durability

### 2. Query Optimization

- Use `filter` context for exact matches
- Leverage `nested` queries for complex relationships
- Implement query caching for frequently used filters
- Use `_source` filtering to return only required fields

### 3. Monitoring

- Track query performance and slow queries
- Monitor index size and shard distribution
- Set up alerts for high memory usage
- Regular index optimization and cleanup

## Data Ingestion Strategy

### 1. ETL Pipeline

1. **Extract**: Parse catalog JSON files
2. **Transform**: 
   - Explode items into separate documents
   - Flatten nested structures
   - Normalize tag structures
3. **Load**: Bulk index into Elasticsearch

### 2. Update Strategy

- **Incremental Updates**: Update only changed items
- **Bulk Operations**: Use bulk API for efficiency
- **Version Control**: Track document versions for consistency
- **Rollback Capability**: Maintain ability to revert changes

## Conclusion

The recommended approach uses a flattened structure with nested objects for complex relationships. This provides:

- Efficient search and filtering
- Good performance characteristics
- Flexible query capabilities
- Scalable architecture for large catalogs

The design balances performance and maintainability while addressing the specific requirements of the Beckn catalog data structure.
