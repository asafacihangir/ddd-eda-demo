package org.phoenix.demo.ordermanagement.application.abstractions;

import java.time.OffsetDateTime;

public interface OutboxProcessedMarker {

    void markPublished(String tenantId, String outboxItemId, OffsetDateTime publishedAt);
}