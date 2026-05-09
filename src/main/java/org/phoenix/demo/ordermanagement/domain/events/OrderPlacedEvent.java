package org.phoenix.demo.ordermanagement.domain.events;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.phoenix.demo.domain.common.event.DomainEvent;
import org.phoenix.demo.ordermanagement.domain.OrderConstants;

public final class OrderPlacedEvent extends DomainEvent {

    private final String orderId;
    private final String customerId;
    private final BigDecimal subtotalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal taxAmount;
    private final BigDecimal totalAmount;
    private final String currency;

    public OrderPlacedEvent(UUID aggregateId,
                            OffsetDateTime occurredOnUtc,
                            String orderId,
                            String customerId,
                            BigDecimal subtotalAmount,
                            BigDecimal discountAmount,
                            BigDecimal taxAmount,
                            BigDecimal totalAmount,
                            String currency) {
        super(aggregateId, occurredOnUtc, OrderConstants.AGGREGATE_TYPE_NAME);
        this.orderId = orderId;
        this.customerId = customerId;
        this.subtotalAmount = subtotalAmount;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.currency = currency;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }
}