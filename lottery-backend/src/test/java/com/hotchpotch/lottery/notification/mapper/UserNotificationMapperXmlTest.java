package com.hotchpotch.lottery.notification.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class UserNotificationMapperXmlTest {

    @Test
    void userNotificationMapperHasMatchingXml() {
        String xml = readResource("mapper/notification/UserNotificationMapper.xml");

        assertThat(xml).contains("namespace=\"" + UserNotificationMapper.class.getName() + "\"");
        assertThat(xml).contains("id=\"BaseResultMap\"");
    }

    @Test
    void userNotificationMapperXmlMapsNotificationColumns() {
        String xml = readResource("mapper/notification/UserNotificationMapper.xml");

        assertThat(xml).contains("column=\"user_id\" property=\"userId\"");
        assertThat(xml).contains("column=\"notification_type\" property=\"notificationType\"");
        assertThat(xml).contains("column=\"business_type\" property=\"businessType\"");
        assertThat(xml).contains("column=\"business_key\" property=\"businessKey\"");
        assertThat(xml).contains("column=\"title\" property=\"title\"");
        assertThat(xml).contains("column=\"content\" property=\"content\"");
        assertThat(xml).contains("column=\"read_status\" property=\"readStatus\"");
        assertThat(xml).contains("column=\"read_time\" property=\"readTime\"");
        assertThat(xml).contains("column=\"create_time\" property=\"createTime\"");
        assertThat(xml).contains("column=\"update_time\" property=\"updateTime\"");
    }

    @Test
    void userNotificationMapperXmlOrdersUnreadBeforeReadNotifications() {
        String xml = readResource("mapper/notification/UserNotificationMapper.xml");

        assertThat(xml).contains("id=\"selectPageByUserId\"");
        assertThat(xml).contains("CASE WHEN read_status = 'UNREAD' THEN 0 ELSE 1 END ASC");
        assertThat(xml).contains("update_time DESC");
        assertThat(xml).contains("LIMIT #{limit} OFFSET #{offset}");
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
