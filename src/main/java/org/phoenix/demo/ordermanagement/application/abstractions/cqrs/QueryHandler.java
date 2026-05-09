package org.phoenix.demo.ordermanagement.application.abstractions.cqrs;

public interface QueryHandler<Q extends Query<R>, R> extends RequestHandler<Q, R> {
}