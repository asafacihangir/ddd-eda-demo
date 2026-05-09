package org.phoenix.demo.ordermanagement.application.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.event.DomainEvent;
import org.phoenix.demo.ordermanagement.application.integration.OrderIntegrationEventMapper.MappedIntegrationEvent;
import org.phoenix.demo.ordermanagement.domain.OrderConstants;
import org.phoenix.demo.ordermanagement.domain.events.OrderCancelledEvent;
import org.phoenix.demo.ordermanagement.domain.events.OrderPlacedEvent;
import org.phoenix.demo.ordermanagement.domain.events.OrderShippedEvent;

class OrderIntegrationEventMapperTest {

    private static final UUID AGGREGATE_ID = UUID.randomUUID();
    private static final OffsetDateTime OCCURRED =
        OffsetDateTime.of(2026, 5, 7, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void map_shouldMapOrderPlacedEvent() {
        OrderPlacedEvent domainEvent = new OrderPlacedEvent(
            AGGREGATE_ID, OCCURRED, "ORD-1", "CUST-1",
            new BigDecimal("100.00"), new BigDecimal("0.00"),
            new BigDecimal("18.00"), new BigDecimal("118.00"), "USD");

        MappedIntegrationEvent result = OrderIntegrationEventMapper.map(domainEvent);

        assertThat(result.eventType()).isEqualTo("OrderPlacedIntegrationEvent");
        assertThat(result.payload()).isInstanceOf(OrderPlacedIntegrationEvent.class);
        OrderPlacedIntegrationEvent payload = (OrderPlacedIntegrationEvent) result.payload();
        assertThat(payload.orderId()).isEqualTo("ORD-1");
        assertThat(payload.totalAmount()).isEqualByComparingTo("118.00");
    }

    @Test
    void map_shouldMapOrderCancelledEvent() {
        OrderCancelledEvent domainEvent =
            new OrderCancelledEvent(AGGREGATE_ID, OCCURRED, "ORD-2");

        MappedIntegrationEvent result = OrderIntegrationEventMapper.map(domainEvent);

        assertThat(result.eventType()).isEqualTo("OrderCancelledIntegrationEvent");
        assertThat(result.payload()).isInstanceOf(OrderCancelledIntegrationEvent.class);
    }

    @Test
    void map_shouldMapOrderShippedEvent() {
        OrderShippedEvent domainEvent =
            new OrderShippedEvent(AGGREGATE_ID, OCCURRED, "ORD-3");

        MappedIntegrationEvent result = OrderIntegrationEventMapper.map(domainEvent);

        assertThat(result.eventType()).isEqualTo("OrderShippedIntegrationEvent");
    }

    @Test
    void map_shouldThrow_forUnknownEvent() {
        DomainEvent unknown = new DomainEvent(AGGREGATE_ID, OCCURRED, OrderConstants.AGGREGATE_TYPE_NAME) { };

        assertThatThrownBy(() -> OrderIntegrationEventMapper.map(unknown))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No integration mapping");
    }
}