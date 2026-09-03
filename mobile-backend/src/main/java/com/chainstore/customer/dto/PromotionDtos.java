package com.chainstore.customer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public final class PromotionDtos {

    private PromotionDtos() {}

    @Data
    public static class PromotionResponse {
        private Long id;
        private String name;
        private String type;
        private String discountLabel;
        private BigDecimal discountValue;
        private Map<String, Object> conditions;
        private String scope;
        private LocalDateTime startAt;
        private LocalDateTime endAt;
        private Integer priority;
    }
}
