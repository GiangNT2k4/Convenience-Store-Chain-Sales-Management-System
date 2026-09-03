package com.chainstore.customer.repository;

import com.chainstore.customer.entity.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CampaignRepository extends JpaRepository<CampaignEntity, Long> {

    @Query("""
            SELECT c FROM CampaignEntity c
            WHERE c.status = 'ACTIVE'
              AND c.startAt <= :now
              AND c.endAt >= :now
            ORDER BY c.priority DESC, c.endAt ASC
            """)
    List<CampaignEntity> findActiveCampaigns(@Param("now") LocalDateTime now);
}
