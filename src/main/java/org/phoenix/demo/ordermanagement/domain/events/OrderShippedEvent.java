package org.phoenix.demo.ordermanagement.domain.events;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.phoenix.demo.domain.common.event.DomainEvent;
import org.phoenix.demo.ordermanagement.domain.OrderConstants;

public final class OrderShippedEvent extends DomainEvent {

    private final String orderId;

    public OrderShippedEvent(UUID aggregateId, OffsetDateTime occurredOnUtc, String orderId) {
        super(aggregateId, occurredOnUtc, OrderConstants.AGGREGATE_TYPE_NAME);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}