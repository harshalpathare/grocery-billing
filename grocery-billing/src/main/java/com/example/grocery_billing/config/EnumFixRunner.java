package com.example.grocery_billing.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EnumFixRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public EnumFixRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            // Automatically fix the ENUM types in the database on startup!
            jdbcTemplate.execute("ALTER TABLE transactions MODIFY COLUMN type ENUM('CREDIT', 'DEBIT') NOT NULL;");
            System.out.println("====== SUCCESS: Updated 'transactions' table ENUM types to CREDIT, DEBIT ======");
            
            jdbcTemplate.execute("ALTER TABLE cash_flow MODIFY COLUMN type ENUM('IN', 'OUT') NOT NULL;");
            System.out.println("====== SUCCESS: Updated 'cash_flow' table ENUM types to IN, OUT ======");
        } catch (Exception e) {
            System.out.println("Note: Database ENUM fix was already applied or could not be applied: " + e.getMessage());
        }
    }
}
