package org.phoenix.demo.ordermanagement.application.abstractions.cqrs;

public interface PipelineBehavior<REQ extends Request<R>, R> {

    R handle(REQ request, Next<R> next);
}