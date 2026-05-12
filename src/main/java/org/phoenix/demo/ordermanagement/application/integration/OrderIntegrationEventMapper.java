package org.phoenix.demo.ordermanagement.application.integration;

import org.phoenix.demo.domain.common.event.DomainEvent;
import org.phoenix.demo.ordermanagement.domain.events.OrderCancelledEvent;
import org.phoenix.demo.ordermanagement.domain.events.OrderPlacedEvent;
import org.phoenix.demo.ordermanagement.domain.events.OrderShippedEvent;

public final class OrderIntegrationEventMapper {

    private OrderIntegrationEventMapper() {
    }

    public static MappedIntegrationEvent map(DomainEvent domainEvent) {
        return switch (domainEvent) {
            case OrderPlacedEvent e -> new MappedIntegrationEvent(
                OrderPlacedIntegrationEvent.class.getSimpleName(),
                new OrderPlacedIntegrationEvent(
                    e.getTenantId(),
                    e.getAggregateId(),
                    e.getOrderId(),
                    e.getCustomerId(),
                    e.getSubtotalAmount(),
                    e.getDiscountAmount(),
                    e.getTaxAmount(),
                    e.getTotalAmount(),
                    e.getCurrency(),
                    e.getOccurredOnUtc()));
            case OrderCancelledEvent e -> new MappedIntegrationEvent(
                OrderCancelledIntegrationEvent.class.getSimpleName(),
                new OrderCancelledIntegrationEvent(
                    e.getTenantId(), e.getAggregateId(), e.getOrderId(), e.getOccurredOnUtc()));
            case OrderShippedEvent e -> new MappedIntegrationEvent(
                OrderShippedIntegrationEvent.class.getSimpleName(),
                new OrderShippedIntegrationEvent(
                    e.getTenantId(), e.getAggregateId(), e.getOrderId(), e.getOccurredOnUtc()));
            default -> throw new IllegalArgumentException(
                "No integration mapping for " + domainEvent.getClass().getName());
        };
    }

    public record MappedIntegrationEvent(String eventType, IntegrationEvent payload) {
    }
}