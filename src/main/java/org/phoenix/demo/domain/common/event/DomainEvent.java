package org.phoenix.demo.domain.common.event;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;


public abstract class DomainEvent {

    private final int version;
    private final UUID eventId;
    private final UUID aggregateId;
    private final OffsetDateTime occurredOnUtc;
    private final String eventType;
    private final String aggregateType;
    private final String traceInfo;

    protected DomainEvent(UUID aggregateId, OffsetDateTime occurredOnUtc, String aggregateTypeName) {
        this(aggregateId, occurredOnUtc, aggregateTypeName, 1, UUID.randomUUID(), null);
    }

    protected DomainEvent(UUID aggregateId,
                          OffsetDateTime occurredOnUtc,
                          String aggregateTypeName,
                          int version,
                          UUID eventId,
                          String traceInfo) {
        this.aggregateId = aggregateId;
        this.occurredOnUtc = occurredOnUtc;
        this.aggregateType = aggregateTypeName;
        this.eventType = aggregateTypeName + "." + getClass().getSimpleName();
        this.version = version;
        this.eventId = eventId;
        this.traceInfo = traceInfo;
    }

    public int getVersion() {
        return version;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public OffsetDateTime getOccurredOnUtc() {
        return occurredOnUtc;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public Optional<String> getTraceInfo() {
        return Optional.ofNullable(traceInfo);
    }
}
