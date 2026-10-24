package com.ijspast.journal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class IjspastJournalApiApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(IjspastJournalApiApplication.class, args);
    }

    private static void loadDotEnv() {
        String[] possiblePaths = {".env", "backend/.env", "../.env"};
        for (String p : possiblePaths) {
            java.io.File envFile = new java.io.File(p);
            if (envFile.exists() && envFile.isFile()) {
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(envFile.toPath());
                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int idx = line.indexOf('=');
                        if (idx > 0) {
                            String key = line.substring(0, idx).trim();
                            String value = line.substring(idx + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            if (!value.isBlank() && System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                    break;
                } catch (Exception ignored) {
                }
            }
        }
    }
}
