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

    @Test
    void v2AllowsSamePrizeGroupWithDifferentPrizeNames() {
        String migration = readMigration("db/migration/V2__adjust_prize_tier_unique_key.sql");

        assertThat(migration).contains("DROP INDEX uk_draw_prize_group");
        assertThat(migration).contains("ADD UNIQUE KEY uk_draw_prize_name (draw_id, prize_name)");
        assertThat(migration).doesNotContainIgnoringCase("FOREIGN KEY");
    }

    @Test
    void v3AddsAsyncSyncTaskProgressColumnsWithoutDatabaseForeignKeys() {
        String migration = readMigration("db/migration/V3__add_sync_task_progress_columns.sql");

        assertThat(migration).contains("ALTER TABLE lottery_sync_tasks");
        assertThat(migration).contains("ADD COLUMN start_page");
        assertThat(migration).contains("ADD COLUMN current_page");
        assertThat(migration).contains("ADD COLUMN last_success_page");
        assertThat(migration).contains("ADD COLUMN failed_page");
        assertThat(migration).contains("ADD COLUMN page_size");
        assertThat(migration).contains("ADD COLUMN max_pages");
        assertThat(migration).contains("ADD COLUMN page_delay_millis");
        assertThat(migration).contains("ADD COLUMN stop_when_last_page");
        assertThat(migration).doesNotContainIgnoringCase("FOREIGN KEY");
    }

    @Test
    void v4CreatesUserAuthTablesWithoutDatabaseForeignKeys() {
        String migration = readMigration("db/migration/V4__create_lottery_user_auth_tables.sql");

        assertThat(migration).contains("CREATE TABLE lottery_users");
        assertThat(migration).contains("CREATE TABLE lottery_user_credentials");
        assertThat(migration).contains("CREATE TABLE lottery_user_oauth_accounts");
        assertThat(migration).contains("CREATE TABLE lottery_user_roles");
        assertThat(migration).contains("avatar_url");
        assertThat(migration).contains("credential_type");
        assertThat(migration).contains("identifier");
        assertThat(migration).contains("password_hash");
        assertThat(migration).contains("verified TINYINT(1) NOT NULL DEFAULT 0");
        assertThat(migration).contains("provider");
        assertThat(migration).contains("open_id");
        assertThat(migration).contains("union_id");
        assertThat(migration).contains("status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'");
        assertThat(migration).contains("last_login_time");
        assertThat(migration).contains("create_time");
        assertThat(migration).contains("update_time");
        assertThat(migration).doesNotContain("username VARCHAR");
        assertThat(migration).contains("UNIQUE KEY uk_lottery_user_credentials_type_identifier (credential_type, identifier)");
        assertThat(migration).contains("UNIQUE KEY uk_lottery_user_oauth_provider_open_id (provider, open_id)");
        assertThat(migration).contains("UNIQUE KEY uk_lottery_user_roles_user_role (user_id, role_code)");
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
