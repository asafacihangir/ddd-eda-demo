package org.phoenix.demo.ordermanagement.application.abstractions.cqrs;

public interface RequestDispatcher {

    <R, REQ extends Request<R>> R dispatch(REQ request);
}