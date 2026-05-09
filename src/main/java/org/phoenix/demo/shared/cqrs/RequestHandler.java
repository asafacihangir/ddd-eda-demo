package org.phoenix.demo.shared.cqrs;

public interface RequestHandler<REQ extends Request<R>, R> {

    R handle(REQ request);
}