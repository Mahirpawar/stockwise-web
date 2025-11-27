package com.stockwise;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class StockwiseApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(StockwiseApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        String dbPath = "data/stockwise.db";
        Path dbFile = Path.of(dbPath);

        // Create directory if not exists
        if (!dbFile.getParent().toFile().exists()) {
            dbFile.getParent().toFile().mkdirs();
        }

        // Check if DB exists
        boolean needInit = !dbFile.toFile().exists();

        // Connection string
        String url = "jdbc:sqlite:" + dbPath;

        try (Connection conn = DriverManager.getConnection(url)) {

            if (needInit) {
                System.out.println("⏳ Creating new SQLite DB…");

                // Load schema.sql from inside JAR using InputStream
                ClassPathResource resource = new ClassPathResource("schema.sql");

                try (InputStream in = resource.getInputStream()) {
                    String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);

                    Statement st = conn.createStatement();
                    st.executeUpdate(sql);
                    st.close();

                    System.out.println("✅ SQLite schema created successfully.");
                }

            } else {
                System.out.println("📌 Using existing SQLite DB at: " + dbPath);
            }
        }
    }
}
