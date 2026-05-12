package org.phoenix.demo.ordermanagement.infra.cosmos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.phoenix.demo.ordermanagement.application.abstractions.OutboxPayloadSerializer;
import org.springframework.stereotype.Component;

@Component
public class JacksonOutboxPayloadSerializer implements OutboxPayloadSerializer {

    private final ObjectMapper objectMapper;

    public JacksonOutboxPayloadSerializer() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
    }
}