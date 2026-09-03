package com.chainstore.customer.service;

import com.chainstore.customer.dto.CustomerDtos;
import com.chainstore.customer.entity.MembershipTierEntity;
import com.chainstore.customer.entity.OrderEntity;
import com.chainstore.customer.entity.PointTransactionEntity;
import com.chainstore.customer.entity.UserEntity;
import com.chainstore.customer.exception.ApiException;
import com.chainstore.customer.repository.OrderRepository;
import com.chainstore.customer.repository.PointTransactionRepository;
import com.chainstore.customer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final OrderRepository orderRepository;
    private final TierService tierService;

    @Value("${loyalty.vnd-per-point:10000}")
    private long vndPerPoint;

    @Value("${loyalty.point-value-vnd:1000}")
    private long pointValueVnd;

    @Transactional
    public CustomerDtos.ProfileResponse getProfile(UserEntity user) {
        MembershipTierEntity tier = tierService.syncUserTier(user);
        long lifetime = tierService.lifetimeEarned(user.getId(), user.getPoints());
        CustomerDtos.ProfileResponse res = new CustomerDtos.ProfileResponse();
        res.setId(user.getId());
        res.setFullName(user.getFullName());
        res.setPhone(user.getPhone());
        res.setEmail(user.getEmail());
        res.setDateOfBirth(user.getDateOfBirth());
        res.setGender(user.getGender());
        res.setPoints(user.getPoints());
        res.setLifetimeEarnedPoints(lifetime);
        res.setMemberSince(user.getCreatedAt());
        res.setQrPayload(user.getPhone());
        if (tier != null) {
            res.setTierCode(tier.getCode());
            res.setTierName(tier.getName());
            res.setPointMultiplier(tier.getPointMultiplier());
            res.setTierBenefits(tierService.parseBenefits(tier.getBenefitsJson()));
        }
        return res;
    }

    @Transactional
    public CustomerDtos.ProfileResponse updateProfile(UserEntity current, CustomerDtos.UpdateProfileRequest req) {
        UserEntity user = userRepository.findById(current.getId())
                .orElseThrow(() -> new ApiException("User not found"));
        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName().trim());
        }
        if (req.getDateOfBirth() != null) {
            user.setDateOfBirth(req.getDateOfBirth());
        }
        if (req.getGender() != null && !req.getGender().isBlank()) {
            user.setGender(req.getGender().trim());
        }
        userRepository.save(user);
        return getProfile(user);
    }

    @Transactional(readOnly = true)
    public CustomerDtos.PageResponse<CustomerDtos.PointHistoryItem> pointHistory(
            Long userId, String type, int page, int size) {
        int pageIdx = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 50);
        String filter = type == null ? "ALL" : type.trim().toUpperCase();

        // Legacy aliases
        if ("EARN".equals(filter) || "REDEEM".equals(filter)) {
            filter = "INVOICE";
        }

        List<CustomerDtos.PointHistoryItem> all = new ArrayList<>();

        if ("ALL".equals(filter) || "INVOICE".equals(filter)) {
            // Load a generous window then paginate in-memory when mixing with refunds
            Page<OrderEntity> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(
                    userId, PageRequest.of(0, 200));
            for (OrderEntity order : orders.getContent()) {
                all.add(toInvoiceItem(order));
            }
        }

        if ("ALL".equals(filter) || "REFUND_REVERSAL".equals(filter) || "REFUND".equals(filter)) {
            Page<PointTransactionEntity> refunds = pointTransactionRepository
                    .findByCustomerIdAndTypeIgnoreCaseOrderByCreatedAtDesc(
                            userId, "REFUND_REVERSAL", PageRequest.of(0, 200));
            for (PointTransactionEntity tx : refunds.getContent()) {
                all.add(toRefundItem(tx));
            }
        }

        all.sort(Comparator.comparing(CustomerDtos.PointHistoryItem::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        long totalElements = all.size();
        int from = Math.min(pageIdx * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        List<CustomerDtos.PointHistoryItem> pageContent = all.subList(from, to);

        CustomerDtos.PageResponse<CustomerDtos.PointHistoryItem> pageRes = new CustomerDtos.PageResponse<>();
        pageRes.setContent(pageContent);
        pageRes.setPage(pageIdx);
        pageRes.setSize(pageSize);
        pageRes.setTotalElements(totalElements);
        pageRes.setTotalPages((int) Math.ceil(totalElements / (double) pageSize));
        return pageRes;
    }

    public CustomerDtos.LoyaltyConfigResponse loyaltyConfig() {
        CustomerDtos.LoyaltyConfigResponse res = new CustomerDtos.LoyaltyConfigResponse();
        res.setVndPerPoint(vndPerPoint);
        res.setPointValueVnd(pointValueVnd);
        return res;
    }

    @Transactional
    public List<CustomerDtos.TierResponse> listTiers(UserEntity user) {
        MembershipTierEntity current = tierService.syncUserTier(user);
        Long currentId = current != null ? current.getId() : null;
        return tierService.allActive().stream().map(t -> {
            CustomerDtos.TierResponse r = new CustomerDtos.TierResponse();
            r.setId(t.getId());
            r.setCode(t.getCode());
            r.setName(t.getName());
            r.setMinPoints(t.getMinPoints());
            r.setMaxPoints(t.getMaxPoints());
            r.setPointMultiplier(t.getPointMultiplier());
            r.setBenefits(tierService.parseBenefits(t.getBenefitsJson()));
            r.setSortOrder(t.getSortOrder());
            r.setCurrent(currentId != null && currentId.equals(t.getId()));
            return r;
        }).collect(Collectors.toList());
    }

    public CustomerDtos.QrResponse qr(UserEntity user) {
        CustomerDtos.QrResponse r = new CustomerDtos.QrResponse();
        r.setPhone(user.getPhone());
        r.setPayload(user.getPhone());
        return r;
    }

    private CustomerDtos.PointHistoryItem toInvoiceItem(OrderEntity order) {
        CustomerDtos.PointHistoryItem item = new CustomerDtos.PointHistoryItem();
        item.setId(order.getId());
        item.setOrderId(order.getId());
        item.setInvoiceCode(order.getInvoiceCode());
        long earned = order.getPointsEarned() != null ? order.getPointsEarned() : 0L;
        item.setPoints(earned);
        item.setPointsEarned(earned);
        item.setOrderTotal(order.getTotal());
        item.setType("INVOICE");
        item.setCreatedAt(order.getCreatedAt());
        String code = order.getInvoiceCode();
        item.setLabel(code != null && !code.isBlank() ? "Invoice " + code : "Purchase #" + order.getId());
        return item;
    }

    private CustomerDtos.PointHistoryItem toRefundItem(PointTransactionEntity tx) {
        CustomerDtos.PointHistoryItem item = new CustomerDtos.PointHistoryItem();
        item.setId(tx.getId());
        item.setOrderId(tx.getOrderId());
        item.setPoints(tx.getPoints());
        item.setPointsEarned(tx.getPoints());
        item.setType("REFUND_REVERSAL");
        item.setCreatedAt(tx.getCreatedAt());
        item.setLabel("Refund adjustment");
        return item;
    }
}
