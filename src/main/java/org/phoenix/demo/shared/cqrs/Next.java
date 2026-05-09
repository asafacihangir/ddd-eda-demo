package org.phoenix.demo.shared.cqrs;

@FunctionalInterface
public interface Next<R> {

    R proceed();
}