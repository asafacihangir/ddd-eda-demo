package org.phoenix.demo.shared.cqrs;

public interface RequestDispatcher {

    <R, REQ extends Request<R>> R dispatch(REQ request);
}