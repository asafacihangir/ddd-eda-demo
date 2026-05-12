package org.phoenix.demo.ordermanagement.application.outbox;

import java.util.Map;

public record OutboxRecord(
        String tenantId,
        String id,
        String orderId,
        String eventType,
        String payloadJson,
        Map<String, String> metadata,
        boolean processed
) {
    public OutboxRecord {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}