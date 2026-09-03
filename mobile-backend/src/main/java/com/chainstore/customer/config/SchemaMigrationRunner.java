package com.chainstore.customer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SchemaMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        createMembershipTiers();
        ensureMembershipTiersColumns();
        seedMembershipTiers();
        addUserColumnIfMissing("membership_tier_id", "BIGINT NULL");
        addUserColumnIfMissing("date_of_birth", "DATE NULL");
        addUserColumnIfMissing("gender", "VARCHAR(32) NULL");
        addUserColumnIfMissing("is_verified", "TINYINT(1) NOT NULL DEFAULT 1");
        ensurePhoneIndex();
        createEmailOtpTable();
        log.info("Customer schema migrations completed.");
    }

    private void createMembershipTiers() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS membership_tiers (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    code VARCHAR(32) NULL,
                    name VARCHAR(64) NOT NULL,
                    min_points BIGINT NOT NULL DEFAULT 0,
                    max_points BIGINT NULL,
                    point_multiplier DOUBLE NOT NULL DEFAULT 1,
                    benefits_json TEXT NULL,
                    sort_order INT NOT NULL DEFAULT 0,
                    active TINYINT(1) NOT NULL DEFAULT 1
                )
                """);
    }

    private void ensureMembershipTiersColumns() {
        addTableColumnIfMissing("membership_tiers", "code", "VARCHAR(32) NULL");
        addTableColumnIfMissing("membership_tiers", "name", "VARCHAR(64) NOT NULL DEFAULT 'Tier'");
        addTableColumnIfMissing("membership_tiers", "min_points", "BIGINT NOT NULL DEFAULT 0");
        addTableColumnIfMissing("membership_tiers", "max_points", "BIGINT NULL");
        addTableColumnIfMissing("membership_tiers", "point_multiplier", "DOUBLE NOT NULL DEFAULT 1");
        addTableColumnIfMissing("membership_tiers", "benefits_json", "TEXT NULL");
        addTableColumnIfMissing("membership_tiers", "sort_order", "INT NOT NULL DEFAULT 0");
        addTableColumnIfMissing("membership_tiers", "active", "TINYINT(1) NOT NULL DEFAULT 1");
        try {
            Integer idx = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_tiers' AND INDEX_NAME = 'uq_membership_tiers_code'
                    """, Integer.class);
            if (idx != null && idx == 0) {
                jdbc.execute("CREATE UNIQUE INDEX uq_membership_tiers_code ON membership_tiers (code)");
            }
        } catch (Exception e) {
            log.warn("Could not create uq_membership_tiers_code: {}", e.getMessage());
        }
    }

    private void seedMembershipTiers() {
        // Deactivate legacy rows without our codes (pre-existing SDS stubs).
        try {
            jdbc.update("UPDATE membership_tiers SET active = 0 WHERE code IS NULL OR code = ''");
        } catch (Exception e) {
            log.warn("Could not deactivate legacy tiers: {}", e.getMessage());
        }

        String silverBenefits = "[\"Priority customer support line\",\"Early access to selected promotions\",\"Birthday reward 100 pts\"]";
        String goldBenefits = "[\"Priority customer support line\",\"Early access to all promotions\",\"Exclusive member-only deals\",\"Birthday reward 200 pts\",\"Free monthly reward item\"]";
        String platinumBenefits = "[\"Priority customer support line\",\"Early access to all promotions\",\"Exclusive member-only deals\",\"Birthday reward 500 pts\",\"Free monthly reward item\",\"Highest point multiplier\"]";

        Integer silver = jdbc.queryForObject(
                "SELECT COUNT(*) FROM membership_tiers WHERE code = 'SILVER'", Integer.class);
        if (silver == null || silver == 0) {
            jdbc.update("""
                    INSERT INTO membership_tiers (code, name, min_points, max_points, point_multiplier, benefits_json, sort_order, active)
                    VALUES
                    ('SILVER', 'Silver', 0, 2499, 1.0, ?, 1, 1),
                    ('GOLD', 'Gold', 2500, 5499, 1.5, ?, 2, 1),
                    ('PLATINUM', 'Platinum', 5500, NULL, 2.0, ?, 3, 1)
                    """, silverBenefits, goldBenefits, platinumBenefits);
            log.info("Seeded membership_tiers (Silver/Gold/Platinum).");
        } else {
            log.info("membership_tiers already seeded — leaving admin-managed values unchanged.");
        }
    }

    private void addUserColumnIfMissing(String column, String definition) {
        addTableColumnIfMissing("users", column, definition);
    }

    private void addTableColumnIfMissing(String table, String column, String definition) {
        Integer exists = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, table, column);
        if (exists != null && exists > 0) {
            return;
        }
        jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        log.info("Added {}.{}", table, column);
    }

    private void ensurePhoneIndex() {
        try {
            Integer idx = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'uq_users_phone'
                    """, Integer.class);
            if (idx != null && idx == 0) {
                jdbc.execute("CREATE UNIQUE INDEX uq_users_phone ON users (phone)");
                log.info("Created unique index uq_users_phone");
            }
        } catch (Exception e) {
            log.warn("Could not create uq_users_phone (duplicates may exist): {}", e.getMessage());
        }
    }

    private void createEmailOtpTable() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS email_otp_tokens (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) NOT NULL,
                    purpose VARCHAR(32) NOT NULL,
                    code_hash VARCHAR(128) NOT NULL,
                    payload TEXT NULL,
                    expires_at DATETIME NOT NULL,
                    attempts INT NOT NULL DEFAULT 0,
                    consumed_at DATETIME NULL,
                    created_at DATETIME NOT NULL
                )
                """);
    }
}
