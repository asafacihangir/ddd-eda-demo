package org.phoenix.demo.domain.common;

import java.time.OffsetDateTime;


public interface AuditableEntity {

    OffsetDateTime getCreatedAtUtc();

    OffsetDateTime getLastModifiedAtUtc();
}
