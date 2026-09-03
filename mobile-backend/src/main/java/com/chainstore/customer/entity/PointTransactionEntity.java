package com.chainstore.customer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "point_transactions")
public class PointTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false)
    private Long points;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
