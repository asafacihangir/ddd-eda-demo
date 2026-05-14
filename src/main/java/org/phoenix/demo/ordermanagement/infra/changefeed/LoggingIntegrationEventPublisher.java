package org.phoenix.demo.ordermanagement.infra.changefeed;

import org.phoenix.demo.ordermanagement.application.abstractions.IntegrationEventPublisher;
import org.phoenix.demo.ordermanagement.infra.worker.WorkerComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@WorkerComponent
@ConditionalOnProperty(name = "app.servicebus.publisher.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingIntegrationEventPublisher implements IntegrationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingIntegrationEventPublisher.class);

    @Override
    public void publishOutbox(String tenantId,
                              String outboxItemId,
                              String orderId,
                              String eventType,
                              String payloadJson) {
        int payloadLength = payloadJson == null ? 0 : payloadJson.length();
        log.info("Publish outbox tenantId={} orderId={} outboxItemId={} eventType={} payloadLength={}",
                tenantId, orderId, outboxItemId, eventType, payloadLength);
    }
}
