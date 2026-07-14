package com.example.grocery_billing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbFix {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/bohara_db?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "Hp@9511896490";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
             
            // Execute the fix for transactions table
            String sqlTransactions = "ALTER TABLE transactions MODIFY COLUMN type ENUM('CREDIT', 'DEBIT') NOT NULL;";
            stmt.executeUpdate(sqlTransactions);
            System.out.println("Successfully updated 'transactions' table ENUM type to CREDIT, DEBIT.");

            // Execute the fix for cash_flow table just in case
            String sqlCashFlow = "ALTER TABLE cash_flow MODIFY COLUMN type ENUM('IN', 'OUT') NOT NULL;";
            stmt.executeUpdate(sqlCashFlow);
            System.out.println("Successfully updated 'cash_flow' table ENUM type to IN, OUT.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
