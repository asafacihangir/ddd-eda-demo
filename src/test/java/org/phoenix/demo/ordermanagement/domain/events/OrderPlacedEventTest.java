package org.phoenix.demo.ordermanagement.domain.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OrderPlacedEventTest {

    @Test
    void constructor_sets_all_fields_and_metadata() {
        UUID aggregateId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        OrderPlacedEvent event = new OrderPlacedEvent(
                aggregateId, now,
                "tenant-1",
                "ORD-1", "CUST-1",
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("9.00"),
                new BigDecimal("99.00"),
                "USD");

        assertEquals(aggregateId, event.getAggregateId());
        assertEquals(now, event.getOccurredOnUtc());
        assertEquals("Order", event.getAggregateType());
        assertEquals("Order.OrderPlacedEvent", event.getEventType());
        assertNotNull(event.getEventId());

        assertEquals("tenant-1", event.getTenantId());
        assertEquals("ORD-1", event.getOrderId());
        assertEquals("CUST-1", event.getCustomerId());
        assertEquals(new BigDecimal("100.00"), event.getSubtotalAmount());
        assertEquals(new BigDecimal("10.00"), event.getDiscountAmount());
        assertEquals(new BigDecimal("9.00"), event.getTaxAmount());
        assertEquals(new BigDecimal("99.00"), event.getTotalAmount());
        assertEquals("USD", event.getCurrency());
    }
}