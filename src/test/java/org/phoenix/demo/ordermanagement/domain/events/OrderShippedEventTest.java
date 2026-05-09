package org.phoenix.demo.ordermanagement.domain.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OrderShippedEventTest {

    @Test
    void constructor_sets_payload_and_metadata() {
        UUID aggregateId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        OrderShippedEvent event = new OrderShippedEvent(aggregateId, now, "ORD-1");

        assertEquals(aggregateId, event.getAggregateId());
        assertEquals(now, event.getOccurredOnUtc());
        assertEquals("Order", event.getAggregateType());
        assertEquals("Order.OrderShippedEvent", event.getEventType());
        assertEquals("ORD-1", event.getOrderId());
    }
}