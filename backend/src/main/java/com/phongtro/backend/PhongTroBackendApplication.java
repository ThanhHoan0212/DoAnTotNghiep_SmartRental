package com.phongtro.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class PhongTroBackendApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(PhongTroBackendApplication.class, args);
    }

    private static void loadDotEnv() {
        Path[] possiblePaths = new Path[]{
                Paths.get(".env"),
                Paths.get("backend", ".env"),
                Paths.get("..", "backend", ".env")
        };

        for (Path path : possiblePaths) {
            if (Files.exists(path)) {
                try {
                    List<String> lines = Files.readAllLines(path);
                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        // Gỡ bỏ dấu nháy đơn hoặc kép nếu có
                        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                    System.out.println("[INFO] Successfully loaded environment variables from: " + path.toAbsolutePath());
                    break;
                } catch (Exception e) {
                    System.err.println("[WARN] Failed to read .env file at " + path + ": " + e.getMessage());
                }
            }
        }
    }
}

