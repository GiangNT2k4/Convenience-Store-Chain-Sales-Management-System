package com.chainstore.customer.controller;

import com.chainstore.customer.dto.AuthDtos;
import com.chainstore.customer.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/request-otp")
    public AuthDtos.MessageResponse requestRegisterOtp(@Valid @RequestBody AuthDtos.RegisterRequestOtp body) {
        return authService.requestRegisterOtp(body);
    }

    @PostMapping("/register/verify")
    public AuthDtos.AuthResponse verifyRegister(@Valid @RequestBody AuthDtos.RegisterVerify body) {
        return authService.verifyRegister(body);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest body) {
        return authService.login(body);
    }

    @PostMapping("/forgot/request-otp")
    public AuthDtos.MessageResponse requestForgotOtp(@Valid @RequestBody AuthDtos.ForgotRequestOtp body) {
        return authService.requestForgotOtp(body);
    }

    @PostMapping("/forgot/verify")
    public AuthDtos.MessageResponse verifyForgot(@Valid @RequestBody AuthDtos.ForgotVerify body) {
        return authService.verifyForgot(body);
    }
}
