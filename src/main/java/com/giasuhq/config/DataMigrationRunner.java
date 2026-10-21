package com.giasuhq.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

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

        // Older production databases still have the removed student_id column.
        // Keep it nullable so the current parent/student_name model can insert
        // classes before the legacy column is safely removed manually.
        runMigration("make legacy student link nullable", () -> jdbcTemplate.execute("""
                ALTER TABLE tutoring_classes
                MODIFY COLUMN student_id BIGINT NULL
                """));

        runMigration("repair tutoring class status constraint", this::repairTutoringClassStatusConstraint);

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

        runMigration("create reviews table", () -> jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS reviews (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tutor_id BIGINT NOT NULL,
                    user_id BIGINT NULL,
                    author_name VARCHAR(255) NOT NULL,
                    rating INT NOT NULL,
                    comment TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_reviews_tutor FOREIGN KEY (tutor_id) REFERENCES tutors(user_id) ON DELETE CASCADE,
                    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
                    INDEX idx_reviews_tutor (tutor_id),
                    INDEX idx_reviews_created_at (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """));

        runMigration("seed initial reviews", () -> {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reviews", Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.update("""
                        INSERT INTO reviews (tutor_id, user_id, author_name, rating, comment, created_at) VALUES
                        (1, 2, 'Trần Văn Minh (Lớp 12)', 5, 'Thầy dạy cực kỳ có tâm! Các bài toán tích phân và lượng giác khó thầy hướng dẫn phương pháp giải rất ngắn gọn và dễ hiểu.', '2026-09-10 14:30:00'),
                        (1, 2, 'Nguyễn Phương Thảo (Lớp 11)', 5, 'Em từ mất gốc môn Vật lý mà sau 2 tháng học cùng thầy đã đạt 8.5 điểm kiểm tra 1 tiết. Cảm ơn thầy rất nhiều ạ!', '2026-09-04 09:15:00'),
                        (1, NULL, 'Bác Hoàng (Phụ huynh em Duy)', 5, 'Thầy Ứng rất đúng giờ, phương pháp sư phạm hiện đại. Con tôi tiến bộ rõ rệt chỉ sau 1 tháng theo học.', '2026-08-25 19:00:00'),
                        (4, 2, 'Lê Bảo Anh (Lớp 12)', 5, 'Cô Lan dạy Hóa Sinh cực đỉnh, giải thích các phản ứng và cơ chế sinh học phân tử rất sâu sắc, giúp em tự tin thi Y Dược.', '2026-09-01 16:45:00'),
                        (5, NULL, 'Nguyễn Quốc Hùng (Lớp 10)', 5, 'Cô Thu có nhiều thí nghiệm trực quan thú vị, truyền cảm hứng môn Sinh học rất tốt.', '2026-09-08 11:20:00'),
                        (6, NULL, 'Phạm Thu Trang (Lớp 12)', 4, 'Thầy Đức luyện đề THPT rất bám sát đề thi thực tế, phân tích các bẫy điểm 9+ cặn kẽ.', '2026-09-12 10:00:00'),
                        (7, NULL, 'Vũ Hải Đăng (Lớp 11)', 5, 'Cô Mai phát âm tiếng Anh chuẩn bản ngữ, cách dạy qua văn hóa nghệ thuật giúp nhớ từ vựng rất lâu.', '2026-09-15 15:30:00')
                        """);
            }
        });
    }

    private void runMigration(String name, Runnable migration) {
        try {
            migration.run();
            log.info("Production migration completed: {}.", name);
        } catch (Exception e) {
            log.warn("Production migration '{}' notice: {}", name, e.getMessage());
        }
    }

    private void repairTutoringClassStatusConstraint() {
        List<String> statusConstraints = jdbcTemplate.query("""
                SELECT tc.CONSTRAINT_NAME
                FROM information_schema.TABLE_CONSTRAINTS tc
                LEFT JOIN information_schema.CHECK_CONSTRAINTS cc
                  ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
                 AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
                  AND tc.TABLE_NAME = 'tutoring_classes'
                  AND tc.CONSTRAINT_TYPE = 'CHECK'
                  AND LOWER(COALESCE(cc.CHECK_CLAUSE, '')) LIKE '%status%'
                """, (rs, rowNum) -> rs.getString("CONSTRAINT_NAME"));

        for (String constraintName : statusConstraints) {
            String escapedName = constraintName.replace("`", "``");
            jdbcTemplate.execute("ALTER TABLE tutoring_classes DROP CHECK `" + escapedName + "`");
        }

        jdbcTemplate.execute("""
                ALTER TABLE tutoring_classes
                ADD CONSTRAINT chk_tutoring_classes_status CHECK (
                    status IN ('PENDING_TUTOR_APPROVAL', 'PENDING_PAYMENT', 'ACTIVE',
                               'DECLINED', 'COMPLETED', 'PAUSED', 'CANCELLED')
                )
                """);
    }
}
