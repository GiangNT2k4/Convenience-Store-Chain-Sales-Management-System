package com.chainstore.customer.service;

import com.chainstore.customer.entity.MembershipTierEntity;
import com.chainstore.customer.entity.UserEntity;
import com.chainstore.customer.repository.MembershipTierRepository;
import com.chainstore.customer.repository.PointTransactionRepository;
import com.chainstore.customer.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TierService {

    private final MembershipTierRepository tierRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public long lifetimeEarned(Long userId, Long balanceFallback) {
        Long sum = pointTransactionRepository.sumEarnedPoints(userId);
        if (sum == null || sum == 0L) {
            return balanceFallback != null ? balanceFallback : 0L;
        }
        return sum;
    }

    public MembershipTierEntity resolveTier(long lifetimeEarned) {
        List<MembershipTierEntity> tiers = tierRepository.findByActiveTrueOrderBySortOrderAsc();
        MembershipTierEntity matched = null;
        for (MembershipTierEntity t : tiers) {
            if (lifetimeEarned >= t.getMinPoints()
                    && (t.getMaxPoints() == null || lifetimeEarned <= t.getMaxPoints())) {
                matched = t;
            }
        }
        if (matched == null && !tiers.isEmpty()) {
            matched = tiers.get(0);
        }
        return matched;
    }

    @Transactional
    public MembershipTierEntity syncUserTier(UserEntity user) {
        long lifetime = lifetimeEarned(user.getId(), user.getPoints());
        MembershipTierEntity tier = resolveTier(lifetime);
        if (tier != null && (user.getMembershipTierId() == null || !user.getMembershipTierId().equals(tier.getId()))) {
            user.setMembershipTierId(tier.getId());
            userRepository.save(user);
        }
        return tier;
    }

    public List<String> parseBenefits(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of(json);
        }
    }

    public List<MembershipTierEntity> allActive() {
        return tierRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    public MembershipTierEntity findById(Long id) {
        if (id == null) {
            return null;
        }
        return tierRepository.findById(id).orElse(null);
    }
}
