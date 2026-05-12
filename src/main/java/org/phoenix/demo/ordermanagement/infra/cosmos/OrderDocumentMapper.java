package org.phoenix.demo.ordermanagement.infra.cosmos;

import java.time.OffsetDateTime;
import java.util.Map;
import org.phoenix.demo.domain.common.EntityId;
import org.phoenix.demo.domain.common.valueobject.Currency;
import org.phoenix.demo.domain.common.valueobject.Money;
import org.phoenix.demo.domain.common.valueobject.MoneyBreakdown;
import org.phoenix.demo.ordermanagement.application.outbox.OutboxRecord;
import org.phoenix.demo.ordermanagement.domain.Order;
import org.phoenix.demo.ordermanagement.domain.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderDocumentMapper {

    public OrderCosmosDocument toDocument(Order order) {
        OrderCosmosDocument doc = new OrderCosmosDocument();
        doc.setId(order.getId().value().toString());
        doc.setType("order");
        doc.setTenantId(order.getTenantId());
        doc.setCustomerId(order.getCustomerId());
        doc.setOrderId(order.getOrderId());
        MoneyBreakdown pricing = order.getPricing();
        doc.setSubtotalAmount(pricing.subtotal().amount());
        doc.setDiscountAmount(pricing.discount().amount());
        doc.setTaxAmount(pricing.tax().amount());
        doc.setTotalAmount(pricing.total().amount());
        doc.setCurrency(pricing.total().currency().code());
        doc.setStatus(order.getStatus().name());
        doc.setCreatedAtUtc(order.getCreatedAtUtc());
        doc.setLastModifiedAtUtc(order.getLastModifiedAtUtc());
        return doc;
    }

    public Order toAggregate(OrderCosmosDocument doc) {
        Currency currency = Currency.create(doc.getCurrency()).getValue();
        Money subtotal = Money.create(doc.getSubtotalAmount(), currency).getValue();
        Money discount = Money.create(doc.getDiscountAmount(), currency).getValue();
        Money tax = Money.create(doc.getTaxAmount(), currency).getValue();
        Money total = Money.create(doc.getTotalAmount(), currency).getValue();
        MoneyBreakdown pricing = new MoneyBreakdown(subtotal, discount, tax, total);

        return Order.rehydrate(
                EntityId.parse(doc.getId()),
                doc.getTenantId(),
                doc.getOrderId(),
                doc.getCustomerId(),
                pricing,
                OrderStatus.valueOf(doc.getStatus()),
                doc.getCreatedAtUtc(),
                doc.getLastModifiedAtUtc());
    }

    public OutboxCosmosDocument toOutboxDocument(OutboxRecord record,
                                                 String customerId,
                                                 OffsetDateTime createdAtUtc) {
        OutboxCosmosDocument doc = new OutboxCosmosDocument();
        doc.setId(record.id());
        doc.setType("outbox");
        doc.setTenantId(record.tenantId());
        doc.setCustomerId(customerId);
        doc.setOrderId(record.orderId());
        doc.setEventType(record.eventType());
        doc.setPayloadJson(record.payloadJson());
        doc.setMetadata(record.metadata() == null ? Map.of() : record.metadata());
        doc.setProcessed(record.processed());
        doc.setCreatedAtUtc(createdAtUtc);
        return doc;
    }
}