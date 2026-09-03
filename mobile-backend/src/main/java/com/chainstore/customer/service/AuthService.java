package com.chainstore.customer.service;

import com.chainstore.customer.dto.AuthDtos;
import com.chainstore.customer.entity.EmailOtpTokenEntity;
import com.chainstore.customer.entity.MembershipTierEntity;
import com.chainstore.customer.entity.UserEntity;
import com.chainstore.customer.exception.ApiException;
import com.chainstore.customer.repository.UserRepository;
import com.chainstore.customer.security.JwtService;
import com.chainstore.customer.util.EmailNormalizer;
import com.chainstore.customer.util.PhoneNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String FORGOT_GENERIC_MSG =
            "If an account exists for this email, a verification code has been sent";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final TierService tierService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${customer.role-id:7}")
    private Long customerRoleId;

    @Transactional
    public AuthDtos.MessageResponse requestRegisterOtp(AuthDtos.RegisterRequestOtp req) {
        String phone = requireValidPhone(req.getPhone());
        String email = requireValidEmail(req.getEmail());
        if (userRepository.existsByPhone(phone)) {
            throw new ApiException("Phone number is already registered");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException("Email is already registered");
        }
        if (req.getPassword().length() < 8) {
            throw new ApiException("Password must be at least 8 characters");
        }
        try {
            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("phone", phone);
            payloadMap.put("fullName", req.getFullName().trim());
            payloadMap.put("passwordHash", passwordEncoder.encode(req.getPassword()));
            String payload = objectMapper.writeValueAsString(payloadMap);
            otpService.issue(email, OtpService.PURPOSE_REGISTER, payload);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Unable to start registration");
        }
        return AuthDtos.MessageResponse.of("Verification code sent to your email");
    }

    @Transactional
    public AuthDtos.AuthResponse verifyRegister(AuthDtos.RegisterVerify req) {
        String email = requireValidEmail(req.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException("Email is already registered");
        }
        EmailOtpTokenEntity token = otpService.verify(email, OtpService.PURPOSE_REGISTER, req.getOtp());
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> payload = objectMapper.readValue(token.getPayload(), Map.class);
            String phone = payload.get("phone");
            if (phone == null || userRepository.existsByPhone(phone)) {
                throw new ApiException("Phone number is already registered");
            }
            UserEntity user = new UserEntity();
            user.setPhone(phone);
            user.setEmail(email);
            user.setFullName(payload.get("fullName"));
            user.setPasswordHash(payload.get("passwordHash"));
            user.setRoleId(customerRoleId);
            user.setStatus("active");
            user.setVerified(true);
            user.setPoints(0L);
            user = userRepository.save(user);
            MembershipTierEntity tier = tierService.syncUserTier(user);
            return toAuthResponse(user, tier);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Registration failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        String identifier = req.resolvedIdentifier();
        if (identifier == null || identifier.isBlank()) {
            throw new ApiException("Email or phone number is required");
        }
        UserEntity user = resolveByIdentifier(identifier);
        if (!customerRoleId.equals(user.getRoleId())) {
            throw new ApiException("This account cannot access the customer app");
        }
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new ApiException("Account is not active");
        }
        if (!Boolean.TRUE.equals(user.getVerified())) {
            throw new ApiException("Account is not verified");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ApiException("Invalid email/phone or password");
        }
        MembershipTierEntity tier = tierService.syncUserTier(user);
        return toAuthResponse(user, tier);
    }

    @Transactional
    public AuthDtos.MessageResponse requestForgotOtp(AuthDtos.ForgotRequestOtp req) {
        String email = EmailNormalizer.normalize(req.getEmail());
        if (!EmailNormalizer.isValid(email)) {
            return AuthDtos.MessageResponse.of(FORGOT_GENERIC_MSG);
        }
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!customerRoleId.equals(user.getRoleId())) {
                return;
            }
            if (!"active".equalsIgnoreCase(user.getStatus())) {
                return;
            }
            otpService.issue(email, OtpService.PURPOSE_RESET, null);
        });
        return AuthDtos.MessageResponse.of(FORGOT_GENERIC_MSG);
    }

    @Transactional
    public AuthDtos.MessageResponse verifyForgot(AuthDtos.ForgotVerify req) {
        String email = requireValidEmail(req.getEmail());
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Invalid email or verification code"));
        if (!customerRoleId.equals(user.getRoleId())) {
            throw new ApiException(
                    "This email belongs to a staff account and cannot reset password in the customer app");
        }
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new ApiException("Account is not active");
        }
        otpService.verify(email, OtpService.PURPOSE_RESET, req.getOtp());
        if (req.getNewPassword().length() < 8) {
            throw new ApiException("Password must be at least 8 characters");
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        return AuthDtos.MessageResponse.of("Password updated successfully");
    }

    private UserEntity resolveByIdentifier(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException("Invalid email/phone or password");
        }
        String trimmed = raw.trim();
        if (trimmed.contains("@")) {
            String email = EmailNormalizer.normalize(trimmed);
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new ApiException("Invalid email/phone or password"));
        }
        String phone = PhoneNormalizer.normalize(trimmed);
        if (!PhoneNormalizer.isValidVnMobile(phone)) {
            throw new ApiException("Invalid email/phone or password");
        }
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApiException("Invalid email/phone or password"));
    }

    private AuthDtos.AuthResponse toAuthResponse(UserEntity user, MembershipTierEntity tier) {
        AuthDtos.AuthResponse res = new AuthDtos.AuthResponse();
        res.setToken(jwtService.generateToken(user.getId(), user.getPhone(), user.getFullName()));
        res.setUserId(user.getId());
        res.setPhone(user.getPhone());
        res.setEmail(user.getEmail());
        res.setFullName(user.getFullName());
        res.setPoints(user.getPoints());
        if (tier != null) {
            res.setTierCode(tier.getCode());
            res.setTierName(tier.getName());
        }
        return res;
    }

    private String requireValidPhone(String raw) {
        String phone = PhoneNormalizer.normalize(raw);
        if (!PhoneNormalizer.isValidVnMobile(phone)) {
            throw new ApiException("Invalid phone number");
        }
        return phone;
    }

    private String requireValidEmail(String raw) {
        String email = EmailNormalizer.normalize(raw);
        if (!EmailNormalizer.isValid(email)) {
            throw new ApiException("Invalid email address");
        }
        return email;
    }
}
