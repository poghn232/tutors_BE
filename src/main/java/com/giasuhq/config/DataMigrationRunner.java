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
        try {
            int updated = jdbcTemplate.update("UPDATE users SET role = 'PARENT' WHERE role = 'STUDENT'");
            if (updated > 0) {
                log.info("Production migration: Converted {} legacy STUDENT accounts to PARENT role.", updated);
            }
            jdbcTemplate.update("INSERT IGNORE INTO parents (user_id) SELECT id FROM users WHERE role = 'PARENT' AND id NOT IN (SELECT user_id FROM parents)");
        } catch (Exception e) {
            log.warn("Production startup migration check notice: {}", e.getMessage());
        }
    }
}
