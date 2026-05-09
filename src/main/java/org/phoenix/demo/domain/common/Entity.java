package org.phoenix.demo.domain.common;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;


public abstract class Entity<T> implements AuditableEntity {

    private final EntityId<T> id;
    private final OffsetDateTime createdAtUtc;
    private final OffsetDateTime lastModifiedAtUtc;

    protected Entity(EntityId<T> id) {
        this(id, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
    }

    protected Entity(EntityId<T> id, OffsetDateTime createdAtUtc, OffsetDateTime lastModifiedAtUtc) {
        this.id = Guards.notNull(id, "id");
        this.createdAtUtc = Guards.notNull(createdAtUtc, "createdAtUtc");
        this.lastModifiedAtUtc = Guards.notNull(lastModifiedAtUtc, "lastModifiedAtUtc");
    }

    public EntityId<T> getId() {
        return id;
    }

    @Override
    public OffsetDateTime getCreatedAtUtc() {
        return createdAtUtc;
    }

    @Override
    public OffsetDateTime getLastModifiedAtUtc() {
        return lastModifiedAtUtc;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Entity<?> other) {
            return Objects.equals(this.id, other.id);
        }
        return false;
    }


    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
