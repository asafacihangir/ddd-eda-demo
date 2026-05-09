package org.phoenix.demo.domain.common.event;

import java.util.Collection;


public interface DomainEventPublisher {

    void publish(DomainEvent event);

    default void publishAll(Collection<? extends DomainEvent> events) {
        events.forEach(this::publish);
    }
}
