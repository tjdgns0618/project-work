package com.example.projectwork.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.projectwork.domain.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
