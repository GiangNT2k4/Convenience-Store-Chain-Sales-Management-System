package com.chainstore.customer.controller;

import com.chainstore.customer.dto.PromotionDtos;
import com.chainstore.customer.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/active")
    public List<PromotionDtos.PromotionResponse> active() {
        return promotionService.listActive();
    }
}
