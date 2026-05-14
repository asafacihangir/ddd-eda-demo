package org.phoenix.demo.ordermanagement.application.outbox;

import java.time.OffsetDateTime;
import java.util.Map;

public record OutboxRecord(
        String tenantId,
        String id,
        String orderId,
        String eventType,
        String payloadJson,
        Map<String, String> metadata,
        boolean processed,
        String status,
        OffsetDateTime publishedAt
) {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PUBLISHED = "PUBLISHED";

    public OutboxRecord {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        status = status == null ? STATUS_PENDING : status;
    }
}