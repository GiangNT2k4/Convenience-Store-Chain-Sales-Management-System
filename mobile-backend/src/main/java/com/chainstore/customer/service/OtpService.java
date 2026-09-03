package com.chainstore.customer.service;

import com.chainstore.customer.config.EmailService;
import com.chainstore.customer.entity.EmailOtpTokenEntity;
import com.chainstore.customer.exception.ApiException;
import com.chainstore.customer.repository.EmailOtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    public static final String PURPOSE_REGISTER = "REGISTER";
    public static final String PURPOSE_RESET = "RESET_PASSWORD";

    private final EmailOtpTokenRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    @Value("${otp.mock:true}")
    private boolean mockMode;

    @Value("${otp.mock-code:123456}")
    private String mockCode;

    @Value("${otp.ttl-seconds:300}")
    private int ttlSeconds;

    @Value("${otp.resend-cooldown-seconds:45}")
    private int resendCooldownSeconds;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public void issue(String email, String purpose, String payloadJson) {
        otpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(email, purpose)
                .ifPresent(existing -> {
                    if (existing.getCreatedAt().plusSeconds(resendCooldownSeconds).isAfter(LocalDateTime.now())) {
                        throw new ApiException("Please wait before requesting another code");
                    }
                    existing.setConsumedAt(LocalDateTime.now());
                    otpRepository.save(existing);
                });

        String code = mockMode
                ? mockCode
                : String.format("%06d", random.nextInt(1_000_000));
        EmailOtpTokenEntity token = new EmailOtpTokenEntity();
        token.setEmail(email);
        token.setPurpose(purpose);
        token.setCodeHash(passwordEncoder.encode(code));
        token.setPayload(payloadJson);
        token.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
        token.setAttempts(0);
        token.setCreatedAt(LocalDateTime.now());
        otpRepository.save(token);

        int minutes = Math.max(1, ttlSeconds / 60);
        String subject = PURPOSE_REGISTER.equals(purpose)
                ? "ChainStore – Confirm your account"
                : "ChainStore – Password reset code";
        String title = PURPOSE_REGISTER.equals(purpose)
                ? "Account verification"
                : "Password reset";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#1a1a1a">
                  <h2 style="margin:0 0 8px;color:#c8102e">ChainStore</h2>
                  <p style="margin:0 0 16px;color:#555">%s</p>
                  <p style="margin:0 0 8px">Your verification code is:</p>
                  <p style="font-size:32px;font-weight:700;letter-spacing:6px;margin:16px 0;color:#111">%s</p>
                  <p style="margin:0;color:#777;font-size:13px">Valid for %d minutes. Do not share this code with anyone.</p>
                </div>
                """.formatted(title, code, minutes);

        try {
            emailService.sendHtmlEmailSync(email, subject, html);
            if (mockMode) {
                log.info("OTP email sent to {} (mock code={})", email, code);
            }
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
            if (mockMode) {
                log.warn("SMTP send failed in mock mode; OTP for {} is still {}", email, code);
                return;
            }
            throw new ApiException("Unable to send verification code. Please try again later.");
        }
    }

    @Transactional
    public EmailOtpTokenEntity verify(String email, String purpose, String otp) {
        EmailOtpTokenEntity token = otpRepository
                .findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new ApiException("No active verification code. Please request a new one."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException("Verification code expired. Please request a new one.");
        }
        if (token.getAttempts() >= maxAttempts) {
            throw new ApiException("Too many invalid attempts. Please request a new code.");
        }

        boolean ok = passwordEncoder.matches(otp, token.getCodeHash())
                || (mockMode && mockCode.equals(otp));
        if (!ok) {
            token.setAttempts(token.getAttempts() + 1);
            otpRepository.save(token);
            throw new ApiException("Invalid verification code");
        }

        token.setConsumedAt(LocalDateTime.now());
        return otpRepository.save(token);
    }
}
