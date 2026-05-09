package org.phoenix.demo.domain.common;

import java.util.UUID;


public final class Guards {

    private static final UUID DEFAULT_UUID = new UUID(0L, 0L);

    private Guards() {
    }


    public static UUID notDefaultUuid(UUID value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException(paramName + " cannot be null.");
        }
        if (DEFAULT_UUID.equals(value)) {
            throw new IllegalArgumentException(paramName + " cannot be default.");
        }
        return value;
    }

    public static <T> T notNull(T value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException(paramName + " cannot be null.");
        }
        return value;
    }

    public static String notBlank(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(paramName + " cannot be blank.");
        }
        return value;
    }
}
