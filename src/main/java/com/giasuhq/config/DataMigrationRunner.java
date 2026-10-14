package com.giasuhq.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        runMigration("create durable SePay confirmations", () -> jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sepay_confirmed_orders (
                    order_code VARCHAR(80) PRIMARY KEY,
                    webhook_id BIGINT NULL,
                    gateway VARCHAR(80) NULL,
                    transaction_date VARCHAR(80) NULL,
                    account_number VARCHAR(80) NULL,
                    transfer_amount BIGINT NULL,
                    reference_code VARCHAR(160) NULL,
                    content TEXT NULL,
                    confirmed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """));

        runMigration("widen tutoring class status", () -> jdbcTemplate.execute("""
                ALTER TABLE tutoring_classes
                MODIFY COLUMN status VARCHAR(40) NOT NULL DEFAULT 'PENDING_TUTOR_APPROVAL'
                """));

        runMigration("convert legacy student roles", () -> {
            int updated = jdbcTemplate.update("UPDATE users SET role = 'PARENT' WHERE role = 'STUDENT'");
            if (updated > 0) {
                log.info("Production migration: Converted {} legacy STUDENT accounts to PARENT role.", updated);
            }
            jdbcTemplate.update("INSERT IGNORE INTO parents (user_id) SELECT id FROM users WHERE role = 'PARENT' AND id NOT IN (SELECT user_id FROM parents)");
        });

        // Keep the first promotion campaigns available when production disables data.sql.
        runMigration("seed default vouchers", () ->
            jdbcTemplate.update("""
                    INSERT IGNORE INTO vouchers
                    (code, name, description, discount_type, discount_value, max_discount, min_order_amount,
                     usage_limit, claimed_count, max_claims_per_user, starts_at, expires_at, active, shareable, auto_issue_on_payment)
                    VALUES
                    ('PAYMENT10', 'Voucher cảm ơn thanh toán', 'Tặng sau khi người dùng hoàn tất thanh toán phí kết nối.',
                     'PERCENTAGE', 10, 100000, 5000, NULL, 0, 1, CURRENT_TIMESTAMP, '2030-12-31 23:59:59', TRUE, TRUE, TRUE),
                    ('SHARE20', 'Voucher giới thiệu bạn bè', 'Chia sẻ link để người nhận được giảm 20% phí kết nối.',
                     'PERCENTAGE', 20, 5000, 5000, NULL, 0, 1, CURRENT_TIMESTAMP, '2030-12-31 23:59:59', TRUE, TRUE, FALSE)
                    """));
    }

    private void runMigration(String name, Runnable migration) {
        try {
            migration.run();
            log.info("Production migration completed: {}.", name);
        } catch (Exception e) {
            log.warn("Production migration '{}' notice: {}", name, e.getMessage());
        }
    }
}
