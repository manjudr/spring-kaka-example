package org.beckn.catalog.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
public class Event {
    private String id;
    private Instant ts;
    private EventType type;
    private Map<String, Object> payload;

    public enum EventType {
        @JsonProperty("CREATE")
        CREATE,
        @JsonProperty("UPDATE")
        UPDATE,
        @JsonProperty("DELETE")
        DELETE
    }
}
