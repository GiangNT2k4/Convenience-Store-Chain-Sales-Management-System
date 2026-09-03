package com.chainstore.customer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read-only mapping of POS orders for customer invoice history. */
@Getter
@Setter
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "points_earned", nullable = false)
    private Long pointsEarned = 0L;

    @Column(name = "points_redeemed", nullable = false)
    private Long pointsRedeemed = 0L;

    @Column(name = "invoice_code", length = 64)
    private String invoiceCode;

    @Column(nullable = false, length = 32)
    private String status = "COMPLETED";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
