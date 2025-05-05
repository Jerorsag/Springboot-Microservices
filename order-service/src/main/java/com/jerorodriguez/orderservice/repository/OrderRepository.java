package com.jerorodriguez.orderservice.repository;

import com.jerorodriguez.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
