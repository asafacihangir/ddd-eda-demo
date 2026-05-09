package org.phoenix.demo.ordermanagement.domain;

public enum OrderStatus {

    PLACED(0),
    SHIPPED(1),
    CANCELLED(2);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}