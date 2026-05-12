package org.phoenix.demo.ordermanagement.application.abstractions;

public interface IntegrationAuditLogger {

    void logPublish(String tenantId,
                    String orderId,
                    String outboxItemId,
                    String eventType,
                    String payloadJson);
}