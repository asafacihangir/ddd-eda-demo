package org.phoenix.demo.ordermanagement.application.integration;

public sealed interface IntegrationEvent
        permits OrderPlacedIntegrationEvent,
                OrderCancelledIntegrationEvent,
                OrderShippedIntegrationEvent {
}