package org.phoenix.demo.domain.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.phoenix.demo.domain.common.event.DomainEvent;

class AggregateRootTest {

    private static final class Order { }

    private static final class OrderCreated extends DomainEvent {
        OrderCreated(UUID aggregateId) {
            super(aggregateId, OffsetDateTime.now(ZoneOffset.UTC), "Order");
        }
    }

    private static final class TestAggregate extends AggregateRoot<Order> {
        TestAggregate(EntityId<Order> id) {
            super(id);
        }

        void doSomething() {
            raiseDomainEvent(new OrderCreated(getId().value()));
        }
    }

    @Test
    void raiseDomainEvent_addsToBuffer() {
        TestAggregate agg = new TestAggregate(EntityId.newId());
        agg.doSomething();
        assertEquals(1, agg.domainEvents().size());
    }

    @Test
    void popDomainEvents_returnsAndClearsBuffer() {
        TestAggregate agg = new TestAggregate(EntityId.newId());
        agg.doSomething();
        agg.doSomething();

        List<DomainEvent> popped = agg.popDomainEvents();
        assertEquals(2, popped.size());
        assertTrue(agg.domainEvents().isEmpty());

        // second pop is empty
        assertTrue(agg.popDomainEvents().isEmpty());
    }

    @Test
    void clearEvents_emptiesBuffer() {
        TestAggregate agg = new TestAggregate(EntityId.newId());
        agg.doSomething();
        agg.clearEvents();
        assertTrue(agg.domainEvents().isEmpty());
    }

    @Test
    void domainEvents_returnsImmutableSnapshot() {
        TestAggregate agg = new TestAggregate(EntityId.newId());
        agg.doSomething();
        List<DomainEvent> snapshot = agg.domainEvents();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(null));
    }
}
