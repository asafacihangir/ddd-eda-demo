package org.phoenix.demo.ordermanagement.application.abstractions;

public interface OutboxProcessedMarker {

    void markProcessed(String tenantId, String outboxItemId);
}