package org.phoenix.demo.ordermanagement.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.phoenix.demo.domain.common.AggregateRoot;
import org.phoenix.demo.domain.common.EntityId;
import org.phoenix.demo.domain.common.Guards;
import org.phoenix.demo.domain.common.error.DomainError;
import org.phoenix.demo.domain.common.result.Result;
import org.phoenix.demo.domain.common.valueobject.MoneyBreakdown;
import org.phoenix.demo.ordermanagement.domain.events.OrderCancelledEvent;
import org.phoenix.demo.ordermanagement.domain.events.OrderPlacedEvent;
import org.phoenix.demo.ordermanagement.domain.events.OrderShippedEvent;

public final class Order extends AggregateRoot<Order> {

    private final String tenantId;
    private final String orderId;
    private final String customerId;
    private final MoneyBreakdown pricing;
    private OrderStatus status;

    private Order(EntityId<Order> id,
                  String tenantId,
                  String orderId,
                  String customerId,
                  MoneyBreakdown pricing,
                  OrderStatus status) {
        super(id);
        this.tenantId = Guards.notBlank(tenantId, "tenantId");
        this.orderId = Guards.notBlank(orderId, "orderId");
        this.customerId = Guards.notBlank(customerId, "customerId");
        this.pricing = Guards.notNull(pricing, "pricing");
        this.status = Guards.notNull(status, "status");
    }

    private Order(EntityId<Order> id,
                  String tenantId,
                  String orderId,
                  String customerId,
                  MoneyBreakdown pricing,
                  OrderStatus status,
                  OffsetDateTime createdAtUtc,
                  OffsetDateTime lastModifiedAtUtc) {
        super(id, createdAtUtc, lastModifiedAtUtc);
        this.tenantId = Guards.notBlank(tenantId, "tenantId");
        this.orderId = Guards.notBlank(orderId, "orderId");
        this.customerId = Guards.notBlank(customerId, "customerId");
        this.pricing = Guards.notNull(pricing, "pricing");
        this.status = Guards.notNull(status, "status");
    }

    public static Result<Order, DomainError> placeNew(String tenantId,
                                                      String orderId,
                                                      String customerId,
                                                      MoneyBreakdown pricing) {
        EntityId<Order> id = EntityId.newId();
        Order order = new Order(id, tenantId, orderId, customerId, pricing, OrderStatus.PLACED);
        order.raiseDomainEvent(new OrderPlacedEvent(
                id.value(),
                OffsetDateTime.now(ZoneOffset.UTC),
                tenantId,
                orderId,
                customerId,
                pricing.subtotal().amount(),
                pricing.discount().amount(),
                pricing.tax().amount(),
                pricing.total().amount(),
                pricing.total().currency().code()));
        return Result.success(order);
    }

    public static Order rehydrate(EntityId<Order> id,
                                  String tenantId,
                                  String orderId,
                                  String customerId,
                                  MoneyBreakdown pricing,
                                  OrderStatus status,
                                  OffsetDateTime createdAtUtc,
                                  OffsetDateTime lastModifiedAtUtc) {
        return new Order(id, tenantId, orderId, customerId, pricing, status, createdAtUtc, lastModifiedAtUtc);
    }

    public Result<Void, DomainError> cancel() {
        if (status != OrderStatus.PLACED) {
            return Result.failure(DomainError.badRequest("Only placed orders can be cancelled."));
        }

        status = OrderStatus.CANCELLED;
        raiseDomainEvent(new OrderCancelledEvent(getId().value(), OffsetDateTime.now(ZoneOffset.UTC), tenantId, orderId));
        return Result.success(null);
    }

    public Result<Void, DomainError> markShipped() {
        if (status != OrderStatus.PLACED) {
            return Result.failure(DomainError.badRequest("Only placed orders can be shipped."));
        }

        status = OrderStatus.SHIPPED;
        raiseDomainEvent(new OrderShippedEvent(getId().value(), OffsetDateTime.now(ZoneOffset.UTC), tenantId, orderId));
        return Result.success(null);
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public MoneyBreakdown getPricing() {
        return pricing;
    }

    public OrderStatus getStatus() {
        return status;
    }
}