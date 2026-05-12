package org.phoenix.demo.ordermanagement.domain.events;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.phoenix.demo.domain.common.event.DomainEvent;
import org.phoenix.demo.ordermanagement.domain.OrderConstants;

public final class OrderCancelledEvent extends DomainEvent {

    private final String tenantId;
    private final String orderId;

    public OrderCancelledEvent(UUID aggregateId, OffsetDateTime occurredOnUtc, String tenantId, String orderId) {
        super(aggregateId, occurredOnUtc, OrderConstants.AGGREGATE_TYPE_NAME);
        this.tenantId = tenantId;
        this.orderId = orderId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getOrderId() {
        return orderId;
    }
}