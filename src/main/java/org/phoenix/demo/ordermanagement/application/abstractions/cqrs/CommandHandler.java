package org.phoenix.demo.ordermanagement.application.abstractions.cqrs;

public interface CommandHandler<C extends Command<R>, R> extends RequestHandler<C, R> {
}