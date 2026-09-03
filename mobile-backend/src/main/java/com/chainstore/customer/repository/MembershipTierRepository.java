package com.chainstore.customer.repository;

import com.chainstore.customer.entity.MembershipTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipTierRepository extends JpaRepository<MembershipTierEntity, Long> {
    List<MembershipTierEntity> findByActiveTrueOrderBySortOrderAsc();
    Optional<MembershipTierEntity> findByCode(String code);
}
