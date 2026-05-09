package org.phoenix.demo.ordermanagement.application.abstractions.cqrs;

public interface RequestHandler<REQ extends Request<R>, R> {

    R handle(REQ request);
}