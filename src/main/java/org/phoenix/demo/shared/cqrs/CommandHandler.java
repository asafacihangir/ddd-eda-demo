package org.phoenix.demo.shared.cqrs;

public interface CommandHandler<C extends Command<R>, R> extends RequestHandler<C, R> {
}