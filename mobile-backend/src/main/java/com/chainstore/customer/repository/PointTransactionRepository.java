package com.chainstore.customer.repository;

import com.chainstore.customer.entity.PointTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointTransactionRepository extends JpaRepository<PointTransactionEntity, Long> {
    Page<PointTransactionEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    Page<PointTransactionEntity> findByCustomerIdAndTypeIgnoreCaseOrderByCreatedAtDesc(
            Long customerId, String type, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointTransactionEntity p WHERE p.customerId = :customerId AND p.type = 'EARN' AND p.points > 0")
    Long sumEarnedPoints(@Param("customerId") Long customerId);
}
