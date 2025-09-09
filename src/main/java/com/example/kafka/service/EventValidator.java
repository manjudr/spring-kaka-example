package com.example.kafka.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventValidator {
    private final ObjectMapper objectMapper;
    private JsonSchema schema;

    @PostConstruct
    public void init() {
        try {
            InputStream schemaStream = new ClassPathResource("schema/event-schema.json").getInputStream();
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            schema = factory.getSchema(schemaStream);
        } catch (Exception e) {
            log.error("Failed to load JSON schema", e);
            throw new RuntimeException("Failed to load JSON schema", e);
        }
    }

    public void validate(String json) {
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            Set<ValidationMessage> validationResult = schema.validate(jsonNode);
            
            if (!validationResult.isEmpty()) {
                String errors = validationResult.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining(", "));
                throw new SchemaValidationException("Schema validation failed: " + errors);
            }
        } catch (Exception e) {
            if (e instanceof SchemaValidationException) {
                throw (SchemaValidationException) e;
            }
            throw new SchemaValidationException("Failed to parse JSON: " + e.getMessage());
        }
    }
}

class SchemaValidationException extends RuntimeException {
    public SchemaValidationException(String message) {
        super(message);
    }
}
