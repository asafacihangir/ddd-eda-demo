package org.phoenix.demo.ordermanagement.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.EntityId;
import org.phoenix.demo.domain.common.error.DomainError;
import org.phoenix.demo.domain.common.error.ErrorType;
import org.phoenix.demo.domain.common.event.DomainEvent;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.domain.common.valueobject.Currency;
import org.phoenix.demo.domain.common.valueobject.Money;
import org.phoenix.demo.domain.common.valueobject.MoneyBreakdown;
import org.phoenix.demo.ordermanagement.domain.events.OrderCancelledEvent;
import org.phoenix.demo.ordermanagement.domain.events.OrderPlacedEvent;
import org.phoenix.demo.ordermanagement.domain.events.OrderShippedEvent;

class OrderTest {

    private static final String TENANT_ID = "tenant-1";

    private static MoneyBreakdown samplePricing() {
        Currency usd = Currency.create("USD").getValue();
        Money subtotal = Money.create(new BigDecimal("100.00"), usd).getValue();
        Money discount = Money.create(new BigDecimal("10.00"), usd).getValue();
        Money tax = Money.create(new BigDecimal("9.00"), usd).getValue();
        return MoneyBreakdown.create(subtotal, discount, tax).getValue();
    }

    @Test
    void placeNew_returns_success_with_placed_status_and_raises_event() {
        MoneyBreakdown pricing = samplePricing();

        Result<Order, DomainError> result = Order.placeNew(TENANT_ID, "ORD-1", "CUST-1", pricing);

        assertTrue(result.isSuccess());
        Order order = result.getValue();
        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(TENANT_ID, order.getTenantId());
        assertEquals("ORD-1", order.getOrderId());
        assertEquals("CUST-1", order.getCustomerId());
        assertSame(pricing, order.getPricing());

        List<DomainEvent> events = order.domainEvents();
        assertEquals(1, events.size());
        OrderPlacedEvent placed = assertInstanceOf(OrderPlacedEvent.class, events.get(0));
        assertEquals(order.getId().value(), placed.getAggregateId());
        assertEquals(TENANT_ID, placed.getTenantId());
        assertEquals("ORD-1", placed.getOrderId());
        assertEquals("CUST-1", placed.getCustomerId());
        assertEquals(new BigDecimal("100.00"), placed.getSubtotalAmount());
        assertEquals(new BigDecimal("10.00"), placed.getDiscountAmount());
        assertEquals(new BigDecimal("9.00"), placed.getTaxAmount());
        assertEquals(new BigDecimal("99.00"), placed.getTotalAmount());
        assertEquals("USD", placed.getCurrency());
    }

    @Test
    void cancel_from_placed_succeeds_and_raises_event() {
        Order order = Order.placeNew(TENANT_ID, "ORD-1", "CUST-1", samplePricing()).getValue();
        order.clearEvents();

        Result<Void, DomainError> result = order.cancel();

        assertTrue(result.isSuccess());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        List<DomainEvent> events = order.domainEvents();
        assertEquals(1, events.size());
        OrderCancelledEvent cancelled = assertInstanceOf(OrderCancelledEvent.class, events.get(0));
        assertEquals(TENANT_ID, cancelled.getTenantId());
        assertEquals("ORD-1", cancelled.getOrderId());
    }

    @Test
    void cancel_from_shipped_fails_with_bad_request() {
        Order order = Order.placeNew(TENANT_ID, "ORD-1", "CUST-1", samplePricing()).getValue();
        order.markShipped();

        Result<Void, DomainError> result = order.cancel();

        assertTrue(result.isFailure());
        assertEquals(ErrorType.BAD_REQUEST, result.getError().type());
        assertEquals("Only placed orders can be cancelled.", result.getError().message());
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void markShipped_from_placed_succeeds_and_raises_event() {
        Order order = Order.placeNew(TENANT_ID, "ORD-1", "CUST-1", samplePricing()).getValue();
        order.clearEvents();

        Result<Void, DomainError> result = order.markShipped();

        assertTrue(result.isSuccess());
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        List<DomainEvent> events = order.domainEvents();
        assertEquals(1, events.size());
        OrderShippedEvent shipped = assertInstanceOf(OrderShippedEvent.class, events.get(0));
        assertEquals(TENANT_ID, shipped.getTenantId());
        assertEquals("ORD-1", shipped.getOrderId());
    }

    @Test
    void markShipped_from_cancelled_fails_with_bad_request() {
        Order order = Order.placeNew(TENANT_ID, "ORD-1", "CUST-1", samplePricing()).getValue();
        order.cancel();

        Result<Void, DomainError> result = order.markShipped();

        assertTrue(result.isFailure());
        assertEquals(ErrorType.BAD_REQUEST, result.getError().type());
        assertEquals("Only placed orders can be shipped.", result.getError().message());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void rehydrate_restores_state_without_raising_events() {
        EntityId<Order> id = EntityId.newId();
        MoneyBreakdown pricing = samplePricing();
        OffsetDateTime created = OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime modified = OffsetDateTime.of(2026, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC);

        Order order = Order.rehydrate(id, TENANT_ID, "ORD-1", "CUST-1", pricing, OrderStatus.SHIPPED, created, modified);

        assertEquals(id, order.getId());
        assertEquals(TENANT_ID, order.getTenantId());
        assertEquals("ORD-1", order.getOrderId());
        assertEquals("CUST-1", order.getCustomerId());
        assertSame(pricing, order.getPricing());
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals(created, order.getCreatedAtUtc());
        assertEquals(modified, order.getLastModifiedAtUtc());
        assertTrue(order.domainEvents().isEmpty());
    }
}