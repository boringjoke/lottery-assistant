package com.hotchpotch.lottery.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LocalDevSqlTest {

    @Test
    void localDevAdminSqlCreatesIdempotentAdminAccountWithoutFlywayAutoMigration() {
        String sql = readRepoFile("sql/local-dev-init-admin.sql");

        assertThat(sql).contains("本地开发初始化管理员账号");
        assertThat(sql).contains("默认账号：admin");
        assertThat(sql).contains("默认密码：Admin@123456");
        assertThat(sql).contains("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
        assertThat(sql).contains("SET @local_admin_username := 'admin'");
        assertThat(sql).contains("INSERT INTO lottery_users");
        assertThat(sql).contains("INSERT INTO lottery_user_credentials");
        assertThat(sql).contains("credential_type");
        assertThat(sql).contains("identifier");
        assertThat(sql).contains("password_hash");
        assertThat(sql).contains("verified");
        assertThat(sql).contains("$2a$10$");
        assertThat(sql).contains("INSERT INTO lottery_user_roles");
        assertThat(sql).contains("'USER'");
        assertThat(sql).contains("'ADMIN'");
        assertThat(sql).contains("ON DUPLICATE KEY UPDATE");
        assertThat(sql).doesNotContain("Admin@123456',");
        assertThat(sql).doesNotContainIgnoringCase("FOREIGN KEY");

        Path flywayMigrationPath = resolveRepoPath(
                "lottery-backend/src/main/resources/db/migration/V5__init_local_dev_admin_user.sql");
        assertThat(flywayMigrationPath).doesNotExist();
    }

    private String readRepoFile(String relativePath) {
        Path path = resolveRepoPath(relativePath);

        assertThat(path)
                .as("本地开发 SQL 文件应存在: %s", relativePath)
                .exists();

        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("读取本地开发 SQL 文件失败: " + relativePath, ex);
        }
    }

    private Path resolveRepoPath(String relativePath) {
        Path workingDir = Path.of("").toAbsolutePath();
        Path fromBackendModule = workingDir.resolve("..").resolve(relativePath).normalize();
        if (Files.exists(fromBackendModule) || workingDir.endsWith("lottery-backend")) {
            return fromBackendModule;
        }

        return workingDir.resolve(relativePath).normalize();
    }
}
