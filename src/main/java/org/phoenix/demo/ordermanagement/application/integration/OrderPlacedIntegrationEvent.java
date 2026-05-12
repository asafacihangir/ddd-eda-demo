package org.phoenix.demo.ordermanagement.application.integration;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderPlacedIntegrationEvent(
        String tenantId,
        UUID aggregateId,
        String orderId,
        String customerId,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime occurredOnUtc
) implements IntegrationEvent {
}