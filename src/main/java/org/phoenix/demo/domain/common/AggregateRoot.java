package org.phoenix.demo.domain.common;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.phoenix.demo.domain.common.event.DomainEvent;


public abstract class AggregateRoot<T> extends Entity<T> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected AggregateRoot(EntityId<T> id) {
        super(id);
    }

    protected AggregateRoot(EntityId<T> id, OffsetDateTime createdAtUtc, OffsetDateTime lastModifiedAtUtc) {
        super(id, createdAtUtc, lastModifiedAtUtc);
    }

    public List<DomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    public List<DomainEvent> popDomainEvents() {
        List<DomainEvent> copy = List.copyOf(domainEvents);
        clearEvents();
        return copy;
    }

    public void clearEvents() {
        domainEvents.clear();
    }

    protected void raiseDomainEvent(DomainEvent domainEvent) {
        Guards.notNull(domainEvent, "domainEvent");
        domainEvents.add(domainEvent);
    }
}
