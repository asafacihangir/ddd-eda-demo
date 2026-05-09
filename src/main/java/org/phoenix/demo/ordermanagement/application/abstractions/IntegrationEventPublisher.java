package org.phoenix.demo.ordermanagement.application.abstractions;

public interface IntegrationEventPublisher {

    void publishOutbox(String outboxItemId,
                       String orderId,
                       String eventType,
                       String payloadJson);
}