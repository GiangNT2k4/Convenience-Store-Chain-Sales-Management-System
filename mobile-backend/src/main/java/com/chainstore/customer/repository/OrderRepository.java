package com.chainstore.customer.repository;

import com.chainstore.customer.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Page<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
}
