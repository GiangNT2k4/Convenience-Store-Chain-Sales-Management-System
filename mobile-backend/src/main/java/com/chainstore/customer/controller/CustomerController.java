package com.chainstore.customer.controller;

import com.chainstore.customer.dto.CustomerDtos;
import com.chainstore.customer.entity.UserEntity;
import com.chainstore.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    public CustomerDtos.ProfileResponse me(@AuthenticationPrincipal UserEntity user) {
        return customerService.getProfile(user);
    }

    @PutMapping("/me")
    public CustomerDtos.ProfileResponse updateMe(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody CustomerDtos.UpdateProfileRequest body) {
        return customerService.updateProfile(user, body);
    }

    @GetMapping("/me/qr")
    public CustomerDtos.QrResponse qr(@AuthenticationPrincipal UserEntity user) {
        return customerService.qr(user);
    }

    @GetMapping("/points/history")
    public CustomerDtos.PageResponse<CustomerDtos.PointHistoryItem> history(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return customerService.pointHistory(user.getId(), type, page, size);
    }

    @GetMapping("/loyalty/config")
    public CustomerDtos.LoyaltyConfigResponse loyaltyConfig() {
        return customerService.loyaltyConfig();
    }

    @GetMapping("/tiers")
    public List<CustomerDtos.TierResponse> tiers(@AuthenticationPrincipal UserEntity user) {
        return customerService.listTiers(user);
    }
}
