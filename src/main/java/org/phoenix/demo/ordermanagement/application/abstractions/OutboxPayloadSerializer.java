package org.phoenix.demo.ordermanagement.application.abstractions;

public interface OutboxPayloadSerializer {

    String serialize(Object payload);
}