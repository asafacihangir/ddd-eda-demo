package org.phoenix.demo.ordermanagement.application.abstractions.repositories;

import java.util.Optional;
import org.phoenix.demo.domain.common.EntityId;
import org.phoenix.demo.ordermanagement.domain.Order;

public interface OrderRepository {

    void add(Order order);

    Optional<Order> findById(EntityId<Order> id);

    void update(Order order);
}