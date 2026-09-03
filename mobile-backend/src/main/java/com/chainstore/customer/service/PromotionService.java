package com.chainstore.customer.service;

import com.chainstore.customer.dto.PromotionDtos;
import com.chainstore.customer.entity.CampaignEntity;
import com.chainstore.customer.repository.CampaignRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final CampaignRepository campaignRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<PromotionDtos.PromotionResponse> listActive() {
        LocalDateTime now = LocalDateTime.now();
        return campaignRepository.findActiveCampaigns(now).stream()
                .map(this::toResponse)
                .toList();
    }

    private PromotionDtos.PromotionResponse toResponse(CampaignEntity c) {
        Map<String, Object> conditions = parseConditions(c.getConditions());
        PromotionDtos.PromotionResponse res = new PromotionDtos.PromotionResponse();
        res.setId(c.getId());
        res.setName(c.getName());
        res.setType(c.getType());
        res.setDiscountValue(c.getDiscountValue());
        res.setDiscountLabel(formatDiscount(c.getType(), c.getDiscountValue(), conditions));
        res.setConditions(conditions);
        res.setScope(c.getScope());
        res.setStartAt(c.getStartAt());
        res.setEndAt(c.getEndAt());
        res.setPriority(c.getPriority());
        return res;
    }

    private Map<String, Object> parseConditions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String formatDiscount(String type, BigDecimal discountValue, Map<String, Object> conditions) {
        if (type == null) {
            return discountValue != null ? discountValue.toPlainString() : "—";
        }
        return switch (type) {
            case "PERCENT" -> stripTrailingZeros(discountValue) + "% OFF";
            case "FIXED_AMOUNT" -> formatVnd(discountValue) + " OFF";
            case "BUY_X_GET_Y" -> formatBuyXGetY(discountValue, conditions);
            default -> discountValue != null ? discountValue.toPlainString() : "—";
        };
    }

    private String formatBuyXGetY(BigDecimal discountValue, Map<String, Object> conditions) {
        Object buy = conditions.get("buyQuantity");
        if (buy == null) {
            buy = conditions.get("buyQty");
        }
        Object get = conditions.get("getQuantity");
        if (get == null) {
            get = conditions.get("getQty");
        }
        if (buy != null && get != null) {
            return "Buy " + buy + " Get " + get;
        }
        return discountValue != null ? "Value: " + stripTrailingZeros(discountValue) : "Special offer";
    }

    private String stripTrailingZeros(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatVnd(BigDecimal value) {
        if (value == null) {
            return "0 ₫";
        }
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(value) + " ₫";
    }
}
