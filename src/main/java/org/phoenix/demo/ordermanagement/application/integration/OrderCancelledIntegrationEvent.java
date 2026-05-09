package org.phoenix.demo.ordermanagement.application.integration;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderCancelledIntegrationEvent(
        UUID aggregateId,
        String orderId,
        OffsetDateTime occurredOnUtc
) implements IntegrationEvent {
}