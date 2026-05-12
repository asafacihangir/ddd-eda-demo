package org.phoenix.demo.ordermanagement.application.abstractions;

public interface IntegrationEventPublisher {

    void publishOutbox(String tenantId,
                       String outboxItemId,
                       String orderId,
                       String eventType,
                       String payloadJson);
}
