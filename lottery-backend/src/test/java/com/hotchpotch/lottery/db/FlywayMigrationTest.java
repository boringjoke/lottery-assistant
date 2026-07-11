package com.hotchpotch.lottery.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FlywayMigrationTest {

    @Test
    void v1CreatesInitialLotteryTablesWithoutDatabaseForeignKeys() {
        String migration = readMigration("db/migration/V1__create_lottery_draw_tables.sql");

        assertThat(migration).contains("CREATE TABLE lottery_draws");
        assertThat(migration).contains("CREATE TABLE lottery_prize_tiers");
        assertThat(migration).contains("CREATE TABLE lottery_sync_tasks");
        assertThat(migration).contains("fetched_time");
        assertThat(migration).contains("create_time");
        assertThat(migration).contains("update_time");
        assertThat(migration).doesNotContainIgnoringCase("FOREIGN KEY");
    }

    private String readMigration(String resourcePath) {
        URL resource = getClass().getClassLoader().getResource(resourcePath);

        assertThat(resource)
                .as("Flyway 迁移脚本应放在 src/main/resources/%s", resourcePath)
                .isNotNull();

        try {
            return new String(resource.openStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("读取 Flyway 迁移脚本失败: " + resourcePath, ex);
        }
    }
}
