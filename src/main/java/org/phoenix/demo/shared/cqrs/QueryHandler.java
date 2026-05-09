package org.phoenix.demo.shared.cqrs;

public interface QueryHandler<Q extends Query<R>, R> extends RequestHandler<Q, R> {
}