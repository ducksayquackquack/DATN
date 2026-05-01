package org.example.datnnhom03.Config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataPatchBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataPatchBootstrap.class);
    private static final String PATCH_KEY = "20260404_align_core_products";
    private static final String PATCH_RELATIVE_PATH = "sql/migrations/20260404_align_core_products.sql";

    private final JdbcTemplate jdbcTemplate;

    public DataPatchBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        ensureHistoryTable();

        Integer alreadyApplied = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM dbo.AppDataPatchHistory WHERE patchKey = ?",
            Integer.class,
            PATCH_KEY
        );
        if (alreadyApplied != null && alreadyApplied > 0) {
            log.info("Data patch {} already applied, skipping.", PATCH_KEY);
            return;
        }

        Path scriptPath = Paths.get(PATCH_RELATIVE_PATH).toAbsolutePath().normalize();
        if (!Files.exists(scriptPath)) {
            log.warn("Data patch script not found at {}. Skipping.", scriptPath);
            return;
        }

        String sql = readScript(scriptPath);
        if (sql.isBlank()) {
            log.warn("Data patch script {} is empty. Skipping.", scriptPath);
            return;
        }

        log.info("Applying data patch {} from {}", PATCH_KEY, scriptPath);
        jdbcTemplate.execute(sql);
        jdbcTemplate.update(
            "INSERT INTO dbo.AppDataPatchHistory (patchKey, executedAt) VALUES (?, SYSDATETIME())",
            PATCH_KEY
        );
        log.info("Data patch {} applied successfully.", PATCH_KEY);
    }

    private void ensureHistoryTable() {
        jdbcTemplate.execute(
            "IF OBJECT_ID('dbo.AppDataPatchHistory', 'U') IS NULL "
                + "BEGIN "
                + "CREATE TABLE dbo.AppDataPatchHistory ("
                + "patchKey NVARCHAR(200) NOT NULL PRIMARY KEY, "
                + "executedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()"
                + "); "
                + "END"
        );
    }

    private String readScript(Path scriptPath) throws IOException {
        String content = Files.readString(scriptPath, StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            return content.substring(1);
        }
        return content;
    }
}
