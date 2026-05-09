package org.phoenix.demo.ordermanagement.application.abstractions.cqrs;

@FunctionalInterface
public interface Next<R> {

    R proceed();
}