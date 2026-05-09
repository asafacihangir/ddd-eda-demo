package org.phoenix.demo.domain.common;

import java.util.UUID;


public record EntityId<T>(UUID value) implements Comparable<EntityId<?>> {

    public EntityId {
        Guards.notDefaultUuid(value, "value");
    }

    public static <T> EntityId<T> newId() {
        return new EntityId<>(UUID.randomUUID());
    }

    public static <T> EntityId<T> of(UUID value) {
        return new EntityId<>(value);
    }

    public static <T> EntityId<T> parse(String value) {
        return new EntityId<>(UUID.fromString(value));
    }


    public static <T> EntityId<T> fromId(EntityId<?> other) {
        return new EntityId<>(other.value);
    }

    @Override
    public int compareTo(EntityId<?> other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
