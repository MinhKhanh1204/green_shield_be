package com.chatbox.chatbox.repository;

import com.chatbox.chatbox.model.Order;

import java.util.List;

public interface OrderRepository extends org.springframework.data.jpa.repository.JpaRepository<Order, Long> {

    List<Order> findAllByOrderByCreatedAtDesc();
}
