package com.chainstore.customer.repository;

import com.chainstore.customer.entity.EmailOtpTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailOtpTokenRepository extends JpaRepository<EmailOtpTokenEntity, Long> {
    Optional<EmailOtpTokenEntity> findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String email, String purpose);
}
