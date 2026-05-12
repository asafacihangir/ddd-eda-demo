package org.phoenix.demo.ordermanagement.domain.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OrderCancelledEventTest {

    @Test
    void constructor_sets_payload_and_metadata() {
        UUID aggregateId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        OrderCancelledEvent event = new OrderCancelledEvent(aggregateId, now, "tenant-1", "ORD-1");

        assertEquals(aggregateId, event.getAggregateId());
        assertEquals(now, event.getOccurredOnUtc());
        assertEquals("Order", event.getAggregateType());
        assertEquals("Order.OrderCancelledEvent", event.getEventType());
        assertEquals("tenant-1", event.getTenantId());
        assertEquals("ORD-1", event.getOrderId());
    }
}