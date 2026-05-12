package org.phoenix.demo.ordermanagement.infra.changefeed;

import org.phoenix.demo.ordermanagement.application.abstractions.IntegrationAuditLogger;
import org.phoenix.demo.ordermanagement.infra.worker.WorkerComponent;
@WorkerComponent
public class NoopIntegrationAuditLogger implements IntegrationAuditLogger {

    @Override
    public void logPublish(String tenantId,
                           String orderId,
                           String outboxItemId,
                           String eventType,
                           String payloadJson) {
    }
}
