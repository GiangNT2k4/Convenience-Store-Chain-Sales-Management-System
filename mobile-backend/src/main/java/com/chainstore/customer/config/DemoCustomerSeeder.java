package com.chainstore.customer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Ensures three demo customers (Silver / Gold / Platinum) for the mobile app.
 * Login: email or phone / password {@value #DEMO_PASSWORD}.
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class DemoCustomerSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "Demo@1234";

    public static final String SILVER_EMAIL = "demo.customer.silver@chainstore.vn";
    public static final String GOLD_EMAIL = "demo.customer.gold@chainstore.vn";
    public static final String PLATINUM_EMAIL = "demo.customer.platinum@chainstore.vn";

    public static final String SILVER_PHONE = "0912345678";
    public static final String GOLD_PHONE = "0912345679";
    public static final String PLATINUM_PHONE = "0912345680";

    private static final String LEGACY_EMAIL = "demo.customer@chainstore.vn";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @Value("${customer.role-id:7}")
    private Long customerRoleId;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Long silverId = ensureDemoUser(
                    SILVER_EMAIL, SILVER_PHONE, "Demo Customer Silver", "SILVER", 0L, LEGACY_EMAIL);
            ensureDemoUser(GOLD_EMAIL, GOLD_PHONE, "Demo Customer Gold", "GOLD", 2500L, null);
            ensureDemoUser(PLATINUM_EMAIL, PLATINUM_PHONE, "Demo Customer Platinum", "PLATINUM", 5500L, null);
            seedDemoInvoices(silverId);
            softLockNonWhitelistedDemoUsers();
            log.info("Demo customers ready: {} / {} / {} (password {})",
                    SILVER_EMAIL, GOLD_EMAIL, PLATINUM_EMAIL, DEMO_PASSWORD);
        } catch (Exception e) {
            log.warn("Demo customer seed skipped: {}", e.getMessage());
        }
    }

    private Long ensureDemoUser(
            String email, String phone, String fullName, String tierCode, long points, String legacyEmail) {
        Long existing = findExistingId(email, phone, legacyEmail);
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        Long tierId = jdbc.query(
                "SELECT id FROM membership_tiers WHERE code = ? LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null,
                tierCode);

        if (existing != null) {
            jdbc.update("""
                    UPDATE users SET
                      full_name = ?, email = ?, phone = ?, password_hash = ?,
                      role_id = ?, status = 'active', is_verified = 1,
                      membership_tier_id = ?, points = ?
                    WHERE id = ?
                    """,
                    fullName, email, phone, hash,
                    customerRoleId, tierId, points, existing);
            return existing;
        }

        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO users
                      (full_name, email, phone, password_hash, status, role_id, points,
                       membership_tier_id, is_verified, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'active', ?, ?, ?, 1, NOW(), NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, hash);
            ps.setLong(5, customerRoleId);
            ps.setLong(6, points);
            if (tierId != null) {
                ps.setLong(7, tierId);
            } else {
                ps.setObject(7, null);
            }
            return ps;
        }, keys);
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to insert demo customer " + email);
        }
        return key.longValue();
    }

    private Long findExistingId(String email, String phone, String legacyEmail) {
        Long byEmailOrPhone = jdbc.query(
                "SELECT id FROM users WHERE email = ? OR phone = ? LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null,
                email, phone);
        if (byEmailOrPhone != null) {
            return byEmailOrPhone;
        }
        if (legacyEmail == null || legacyEmail.isBlank()) {
            return null;
        }
        return jdbc.query(
                "SELECT id FROM users WHERE email = ? LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null,
                legacyEmail);
    }

    private void softLockNonWhitelistedDemoUsers() {
        List<Long> ids = jdbc.query("""
                SELECT id FROM users
                WHERE (LOWER(email) LIKE '%demo%' OR LOWER(full_name) LIKE '%demo%')
                  AND LOWER(email) NOT IN (?, ?, ?, ?, ?)
                  AND LOWER(status) = 'active'
                """,
                (rs, rowNum) -> rs.getLong(1),
                SILVER_EMAIL.toLowerCase(Locale.ROOT),
                GOLD_EMAIL.toLowerCase(Locale.ROOT),
                PLATINUM_EMAIL.toLowerCase(Locale.ROOT),
                "demo_cashier@chainstore.vn",
                "demo_is@chainstore.vn");
        for (Long id : ids) {
            jdbc.update("UPDATE users SET status = 'locked' WHERE id = ?", id);
            log.info("Soft-locked non-whitelist demo user id={}", id);
        }
    }

    private void seedDemoInvoices(Long customerId) {
        Integer already = jdbc.queryForObject("""
                SELECT COUNT(*) FROM orders
                WHERE customer_id = ? AND invoice_code LIKE 'DEMO-CS-%'
                """, Integer.class, customerId);
        if (already != null && already > 0) {
            syncPointsFromDemoOrders(customerId);
            return;
        }

        Long branchId = jdbc.query(
                "SELECT id FROM branches ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null);
        Long cashierId = jdbc.query(
                "SELECT id FROM users WHERE role_id != ? ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null,
                customerRoleId);

        if (branchId == null || cashierId == null) {
            log.warn("Demo invoices skipped: need at least one branch and one staff user");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        insertInvoice(branchId, cashierId, customerId, "DEMO-CS-001",
                new BigDecimal("85000"), 8L, now.minusDays(12).withHour(10).withMinute(15));
        insertInvoice(branchId, cashierId, customerId, "DEMO-CS-002",
                new BigDecimal("156000"), 15L, now.minusDays(5).withHour(18).withMinute(40));
        insertInvoice(branchId, cashierId, customerId, "DEMO-CS-003",
                new BigDecimal("42000"), 4L, now.minusDays(1).withHour(9).withMinute(5));
        insertInvoice(branchId, cashierId, customerId, "DEMO-CS-004",
                new BigDecimal("230000"), 23L, now.minusHours(6));

        // Keep silver at tier floor; sample invoices are for history UI only.
        jdbc.update("UPDATE users SET points = 0 WHERE id = ?", customerId);
    }

    private void insertInvoice(
            Long branchId, Long cashierId, Long customerId,
            String invoiceCode, BigDecimal total, long pointsEarned, LocalDateTime createdAt) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO orders
                      (branch_id, shift_id, cashier_id, customer_id, subtotal, discount_amount, total,
                       points_redeemed, points_earned, invoice_code, status, created_at)
                    VALUES (?, NULL, ?, ?, ?, 0, ?, 0, ?, ?, 'COMPLETED', ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, branchId);
            ps.setLong(2, cashierId);
            ps.setLong(3, customerId);
            ps.setBigDecimal(4, total);
            ps.setBigDecimal(5, total);
            ps.setLong(6, pointsEarned);
            ps.setString(7, invoiceCode);
            ps.setTimestamp(8, Timestamp.valueOf(createdAt));
            return ps;
        }, keys);

        Number orderKey = keys.getKey();
        if (orderKey == null) {
            return;
        }
        long orderId = orderKey.longValue();

        jdbc.update("""
                INSERT INTO payments
                  (order_id, method, amount, cash_received, change_amount, transaction_ref, status, created_at)
                VALUES (?, 'CASH', ?, ?, 0, NULL, 'SUCCESS', ?)
                """,
                orderId, total, total, Timestamp.valueOf(createdAt));

        jdbc.update("""
                INSERT INTO point_transactions (customer_id, order_id, points, type, created_at)
                VALUES (?, ?, ?, 'EARN', ?)
                """,
                customerId, orderId, pointsEarned, Timestamp.valueOf(createdAt));
    }

    private void syncPointsFromDemoOrders(Long customerId) {
        // Prefer explicit silver floor over summing demo invoices so tier stays Silver.
        jdbc.update("UPDATE users SET points = 0 WHERE id = ?", customerId);
    }
}
