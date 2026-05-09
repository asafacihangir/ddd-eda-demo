package org.phoenix.demo.domain.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class EntityTest {

    private static final class Order { }

    private static final class TestEntity extends Entity<Order> {
        TestEntity(EntityId<Order> id) {
            super(id);
        }
    }

    private static final class OtherTestEntity extends Entity<Order> {
        OtherTestEntity(EntityId<Order> id) {
            super(id);
        }
    }

    @Test
    void equality_isByIdOnly_evenAcrossSubclasses() {
        EntityId<Order> id = EntityId.newId();
        TestEntity a = new TestEntity(id);
        OtherTestEntity b = new OtherTestEntity(id);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentIds_areNotEqual() {
        TestEntity a = new TestEntity(EntityId.newId());
        TestEntity b = new TestEntity(EntityId.newId());
        assertNotEquals(a, b);
    }

    @Test
    void timestamps_areInitializedOnConstruction() {
        TestEntity e = new TestEntity(EntityId.newId());
        assertNotNull(e.getCreatedAtUtc());
        assertNotNull(e.getLastModifiedAtUtc());
    }
}
