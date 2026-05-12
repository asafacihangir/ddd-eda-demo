package org.phoenix.demo.ordermanagement.application.orders.queries;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderSummaryDto(
        String id,
        String tenantId,
        String orderId,
        String customerId,
        String status,
        BigDecimal total,
        String currency,
        OffsetDateTime createdAtUtc,
        OffsetDateTime lastModifiedAtUtc
) {
}