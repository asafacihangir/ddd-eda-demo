package org.phoenix.demo.ordermanagement.application.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.event.DomainEvent;
import org.phoenix.demo.ordermanagement.application.abstractions.OutboxPayloadSerializer;
import org.phoenix.demo.ordermanagement.domain.events.OrderCancelledEvent;
import org.phoenix.demo.ordermanagement.domain.events.OrderPlacedEvent;

class OutboxMapperTest {

    private static final UUID AGGREGATE_ID = UUID.randomUUID();
    private static final OffsetDateTime OCCURRED =
        OffsetDateTime.of(2026, 5, 7, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final String TENANT_ID = "tenant-1";

    @Test
    void toOutboxRecords_shouldMapEachEventToOneRecord() {
        StubSerializer serializer = new StubSerializer();
        OutboxMapper mapper = new OutboxMapper(serializer);

        List<DomainEvent> events = List.of(
            new OrderPlacedEvent(AGGREGATE_ID, OCCURRED, TENANT_ID, "ORD-1", "CUST-1",
                new BigDecimal("100.00"), new BigDecimal("0.00"),
                new BigDecimal("18.00"), new BigDecimal("118.00"), "USD"),
            new OrderCancelledEvent(AGGREGATE_ID, OCCURRED, TENANT_ID, "ORD-1"));

        List<OutboxRecord> records = mapper.toOutboxRecords(events, TENANT_ID, "ORD-1");

        assertThat(records).hasSize(2);
        assertThat(records).allSatisfy(r -> {
            assertThat(r.tenantId()).isEqualTo(TENANT_ID);
            assertThat(r.id()).isNotBlank();
            assertThat(r.orderId()).isEqualTo("ORD-1");
            assertThat(r.processed()).isFalse();
            assertThat(r.status()).isEqualTo(OutboxRecord.STATUS_PENDING);
            assertThat(r.publishedAt()).isNull();
            assertThat(r.payloadJson()).startsWith("STUB:");
            assertThat(r.metadata()).isEmpty();
        });
        assertThat(records.get(0).eventType()).isEqualTo("OrderPlacedIntegrationEvent");
        assertThat(records.get(1).eventType()).isEqualTo("OrderCancelledIntegrationEvent");
    }

    @Test
    void toOutboxRecords_shouldReturnEmpty_whenNoEvents() {
        OutboxMapper mapper = new OutboxMapper(new StubSerializer());

        List<OutboxRecord> records = mapper.toOutboxRecords(List.of(), TENANT_ID, "ORD-X");

        assertThat(records).isEmpty();
    }

    private static class StubSerializer implements OutboxPayloadSerializer {
        @Override
        public String serialize(Object payload) {
            return "STUB:" + payload.getClass().getSimpleName();
        }
    }
}