package com.hotchpotch.lottery.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LotteryUserMapperXmlTest {

    @Test
    void lotteryUserMapperHasMatchingXml() {
        assertMapperXml("LotteryUserMapper.xml", LotteryUserMapper.class);
    }

    @Test
    void lotteryUserRoleMapperHasMatchingXml() {
        assertMapperXml("LotteryUserRoleMapper.xml", LotteryUserRoleMapper.class);
    }

    @Test
    void lotteryUserCredentialMapperHasMatchingXml() {
        assertMapperXml("LotteryUserCredentialMapper.xml", LotteryUserCredentialMapper.class);
    }

    @Test
    void lotteryUserOAuthAccountMapperHasMatchingXml() {
        assertMapperXml("LotteryUserOAuthAccountMapper.xml", LotteryUserOAuthAccountMapper.class);
    }

    @Test
    void lotteryUserMapperXmlMapsUserProfileColumns() {
        String xml = readResource("mapper/user/LotteryUserMapper.xml");

        assertThat(xml).contains("column=\"nickname\" property=\"nickname\"");
        assertThat(xml).contains("column=\"avatar_url\" property=\"avatarUrl\"");
        assertThat(xml).contains("column=\"status\" property=\"status\"");
        assertThat(xml).contains("column=\"email_notification_enabled\" property=\"emailNotificationEnabled\"");
        assertThat(xml).contains("column=\"last_login_time\" property=\"lastLoginTime\"");
        assertThat(xml).contains("column=\"create_time\" property=\"createTime\"");
        assertThat(xml).contains("column=\"update_time\" property=\"updateTime\"");
    }

    @Test
    void lotteryUserRoleMapperXmlMapsRoleColumns() {
        String xml = readResource("mapper/user/LotteryUserRoleMapper.xml");

        assertThat(xml).contains("column=\"user_id\" property=\"userId\"");
        assertThat(xml).contains("column=\"role_code\" property=\"roleCode\"");
        assertThat(xml).contains("column=\"create_time\" property=\"createTime\"");
        assertThat(xml).contains("column=\"update_time\" property=\"updateTime\"");
    }

    @Test
    void lotteryUserCredentialMapperXmlMapsCredentialColumns() {
        String xml = readResource("mapper/user/LotteryUserCredentialMapper.xml");

        assertThat(xml).contains("column=\"user_id\" property=\"userId\"");
        assertThat(xml).contains("column=\"credential_type\" property=\"credentialType\"");
        assertThat(xml).contains("column=\"identifier\" property=\"identifier\"");
        assertThat(xml).contains("column=\"password_hash\" property=\"passwordHash\"");
        assertThat(xml).contains("column=\"verified\" property=\"verified\"");
        assertThat(xml).contains("column=\"create_time\" property=\"createTime\"");
        assertThat(xml).contains("column=\"update_time\" property=\"updateTime\"");
    }

    @Test
    void lotteryUserOAuthAccountMapperXmlMapsOAuthAccountColumns() {
        String xml = readResource("mapper/user/LotteryUserOAuthAccountMapper.xml");

        assertThat(xml).contains("column=\"user_id\" property=\"userId\"");
        assertThat(xml).contains("column=\"provider\" property=\"provider\"");
        assertThat(xml).contains("column=\"open_id\" property=\"openId\"");
        assertThat(xml).contains("column=\"union_id\" property=\"unionId\"");
        assertThat(xml).contains("column=\"nickname\" property=\"nickname\"");
        assertThat(xml).contains("column=\"avatar_url\" property=\"avatarUrl\"");
        assertThat(xml).contains("column=\"create_time\" property=\"createTime\"");
        assertThat(xml).contains("column=\"update_time\" property=\"updateTime\"");
    }

    private void assertMapperXml(String fileName, Class<?> mapperType) {
        String resourcePath = "mapper/user/" + fileName;
        String xml = readResource(resourcePath);

        assertThat(xml).contains("namespace=\"" + mapperType.getName() + "\"");
        assertThat(xml).contains("id=\"BaseResultMap\"");
    }

    private String readResource(String resourcePath) {
        URL resource = getClass().getClassLoader().getResource(resourcePath);

        assertThat(resource)
                .as("Mapper XML 应放在 src/main/resources/%s", resourcePath)
                .isNotNull();

        try {
            return new String(resource.openStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("读取 Mapper XML 失败: " + resourcePath, ex);
        }
    }
}
