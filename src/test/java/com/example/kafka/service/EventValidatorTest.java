package com.example.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventValidatorTest {
    private EventValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new EventValidator(objectMapper);
        ReflectionTestUtils.invokeMethod(validator, "init");
    }

    @Test
    void validateValidEvent() {
        String validEvent = String.format("""
            {
                "id": "%s",
                "ts": "2025-09-02T10:00:00Z",
                "type": "CREATE",
                "payload": {
                    "name": "test",
                    "value": 123
                }
            }
            """, UUID.randomUUID());

        assertDoesNotThrow(() -> validator.validate(validEvent));
    }

    @Test
    void validateInvalidEventMissingId() {
        String invalidEvent = """
            {
                "ts": "2025-09-02T10:00:00Z",
                "type": "CREATE",
                "payload": {
                    "name": "test"
                }
            }
            """;

        assertThrows(SchemaValidationException.class, () -> validator.validate(invalidEvent));
    }

    @Test
    void validateInvalidEventType() {
        String invalidEvent = String.format("""
            {
                "id": "%s",
                "ts": "2025-09-02T10:00:00Z",
                "type": "INVALID",
                "payload": {
                    "name": "test"
                }
            }
            """, UUID.randomUUID());

        assertThrows(SchemaValidationException.class, () -> validator.validate(invalidEvent));
    }

    @Test
    void validateInvalidTimestamp() {
        String invalidEvent = String.format("""
            {
                "id": "%s",
                "ts": "invalid-date",
                "type": "CREATE",
                "payload": {
                    "name": "test"
                }
            }
            """, UUID.randomUUID());

        assertThrows(SchemaValidationException.class, () -> validator.validate(invalidEvent));
    }
}
