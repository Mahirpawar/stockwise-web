package com.stockwise;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Main Spring Boot Application.
 *
 * IMPORTANT: We disable Spring's default DataSource auto-configuration
 * because we are manually using SQLite through JDBC (DriverManager)
 * and NOT through Spring Boot’s datasource/HikariCP.
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class StockwiseApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(StockwiseApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        String dbPath = "data/stockwise.db";
        Path dbFile = Path.of(dbPath);

        // create /data folder if missing
        if (!dbFile.getParent().toFile().exists()) {
            dbFile.getParent().toFile().mkdirs();
        }

        boolean needInit = !dbFile.toFile().exists();

        // SQLite will auto-create the DB file when connecting
        String url = "jdbc:sqlite:" + dbPath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (needInit) {
                ClassPathResource r = new ClassPathResource("schema.sql");
                String sql = Files.readString(r.getFile().toPath());

                Statement st = conn.createStatement();
                st.executeUpdate(sql);
                st.close();

                System.out.println("✅ Initialized new SQLite DB at: " + dbPath);
            } else {
                System.out.println("📌 Using existing SQLite DB at: " + dbPath);
            }
        }
    }
}
