package org.phoenix.demo.ordermanagement.infra.cosmos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.valueobject.Currency;
import org.phoenix.demo.domain.common.valueobject.Money;
import org.phoenix.demo.domain.common.valueobject.MoneyBreakdown;
import org.phoenix.demo.ordermanagement.application.outbox.OutboxRecord;
import org.phoenix.demo.ordermanagement.domain.Order;
import org.phoenix.demo.ordermanagement.domain.OrderStatus;

class OrderDocumentMapperTest {

    private static final String TENANT_ID = "tenant-1";

    private final OrderDocumentMapper mapper = new OrderDocumentMapper();

    @Test
    void toDocument_mapsAllFields() {
        Order order = newPlacedOrder("ORD-1", "cust-1");

        OrderCosmosDocument doc = mapper.toDocument(order);

        assertThat(doc.getId()).isEqualTo(order.getId().value().toString());
        assertThat(doc.getType()).isEqualTo("order");
        assertThat(doc.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(doc.getCustomerId()).isEqualTo("cust-1");
        assertThat(doc.getOrderId()).isEqualTo("ORD-1");
        assertThat(doc.getSubtotalAmount()).isEqualByComparingTo("100.00");
        assertThat(doc.getDiscountAmount()).isEqualByComparingTo("10.00");
        assertThat(doc.getTaxAmount()).isEqualByComparingTo("18.00");
        assertThat(doc.getTotalAmount()).isEqualByComparingTo("108.00");
        assertThat(doc.getCurrency()).isEqualTo("TRY");
        assertThat(doc.getStatus()).isEqualTo("PLACED");
        assertThat(doc.getCreatedAtUtc()).isNotNull();
        assertThat(doc.getLastModifiedAtUtc()).isNotNull();
    }

    @Test
    void toAggregate_roundtripsOrder() {
        Order original = newPlacedOrder("ORD-2", "cust-2");
        OrderCosmosDocument doc = mapper.toDocument(original);

        Order restored = mapper.toAggregate(doc);

        assertThat(restored.getId()).isEqualTo(original.getId());
        assertThat(restored.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(restored.getOrderId()).isEqualTo("ORD-2");
        assertThat(restored.getCustomerId()).isEqualTo("cust-2");
        assertThat(restored.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(restored.getPricing().total().amount()).isEqualByComparingTo("108.00");
        assertThat(restored.getPricing().total().currency().code()).isEqualTo("TRY");
    }

    @Test
    void toOutboxDocument_mapsAllFields() {
        OutboxRecord record = new OutboxRecord(
                TENANT_ID,
                "outbox-1",
                "ORD-3",
                "OrderPlacedIntegrationEvent",
                "{\"foo\":\"bar\"}",
                Map.of("traceId", "abc"),
                false);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OutboxCosmosDocument doc = mapper.toOutboxDocument(record, "cust-3", now);

        assertThat(doc.getId()).isEqualTo("outbox-1");
        assertThat(doc.getType()).isEqualTo("outbox");
        assertThat(doc.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(doc.getCustomerId()).isEqualTo("cust-3");
        assertThat(doc.getOrderId()).isEqualTo("ORD-3");
        assertThat(doc.getEventType()).isEqualTo("OrderPlacedIntegrationEvent");
        assertThat(doc.getPayloadJson()).isEqualTo("{\"foo\":\"bar\"}");
        assertThat(doc.getMetadata()).containsEntry("traceId", "abc");
        assertThat(doc.isProcessed()).isFalse();
        assertThat(doc.getCreatedAtUtc()).isEqualTo(now);
    }

    private static Order newPlacedOrder(String orderId, String customerId) {
        Money sub = Money.create(new BigDecimal("100.00"), Currency.TRY).getValue();
        Money disc = Money.create(new BigDecimal("10.00"), Currency.TRY).getValue();
        Money tax = Money.create(new BigDecimal("18.00"), Currency.TRY).getValue();
        MoneyBreakdown pricing = MoneyBreakdown.create(sub, disc, tax).getValue();
        return Order.placeNew(TENANT_ID, orderId, customerId, pricing).getValue();
    }
}