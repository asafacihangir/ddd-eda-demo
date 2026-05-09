package org.phoenix.demo.shared.cqrs;

public interface PipelineBehavior<REQ extends Request<R>, R> {

    R handle(REQ request, Next<R> next);
}