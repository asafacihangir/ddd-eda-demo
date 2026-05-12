package org.phoenix.demo.ordermanagement.application.outbox;

import java.util.List;
import java.util.UUID;
import org.phoenix.demo.domain.common.event.DomainEvent;
import org.phoenix.demo.ordermanagement.application.abstractions.OutboxPayloadSerializer;
import org.phoenix.demo.ordermanagement.application.integration.OrderIntegrationEventMapper;
import org.phoenix.demo.ordermanagement.application.integration.OrderIntegrationEventMapper.MappedIntegrationEvent;

public final class OutboxMapper {

    private final OutboxPayloadSerializer serializer;

    public OutboxMapper(OutboxPayloadSerializer serializer) {
        this.serializer = serializer;
    }

    public List<OutboxRecord> toOutboxRecords(List<? extends DomainEvent> events,
                                              String tenantId,
                                              String orderId) {
        return events.stream()
            .map(e -> {
                MappedIntegrationEvent mapped = OrderIntegrationEventMapper.map(e);
                return new OutboxRecord(
                    tenantId,
                    UUID.randomUUID().toString(),
                    orderId,
                    mapped.eventType(),
                    serializer.serialize(mapped.payload()),
                    null,
                    false);
            })
            .toList();
    }
}