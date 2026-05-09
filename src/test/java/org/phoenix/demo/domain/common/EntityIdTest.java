package org.phoenix.demo.domain.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntityIdTest {

    private static final class Order { }

    @Test
    void newId_generatesNonNullUniqueValues() {
        EntityId<Order> a = EntityId.newId();
        EntityId<Order> b = EntityId.newId();
        assertNotNull(a.value());
        assertNotEquals(a, b);
    }

    @Test
    void of_rejectsDefaultUuid() {
        UUID zero = new UUID(0L, 0L);
        assertThrows(IllegalArgumentException.class, () -> EntityId.of(zero));
    }

    @Test
    void of_rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> EntityId.of(null));
    }

    @Test
    void parse_acceptsValidUuidString() {
        UUID uuid = UUID.randomUUID();
        EntityId<Order> id = EntityId.parse(uuid.toString());
        assertEquals(uuid, id.value());
    }

    @Test
    void parse_rejectsInvalidString() {
        assertThrows(IllegalArgumentException.class, () -> EntityId.parse("not-a-uuid"));
    }

    @Test
    void fromId_reTagsValueWithSameUuid() {
        EntityId<Order> source = EntityId.newId();
        EntityId<EntityIdTest> retagged = EntityId.fromId(source);
        assertEquals(source.value(), retagged.value());
    }
}
