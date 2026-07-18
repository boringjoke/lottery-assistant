package com.hotchpotch.lottery.favorite.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LotteryNumberFavoriteMapperXmlTest {

    @Test
    void lotteryNumberFavoriteMapperHasMatchingXml() {
        String xml = readResource("mapper/favorite/LotteryNumberFavoriteMapper.xml");

        assertThat(xml).contains("namespace=\"" + LotteryNumberFavoriteMapper.class.getName() + "\"");
        assertThat(xml).contains("id=\"BaseResultMap\"");
    }

    @Test
    void lotteryNumberFavoriteMapperXmlMapsFavoriteColumns() {
        String xml = readResource("mapper/favorite/LotteryNumberFavoriteMapper.xml");

        assertThat(xml).contains("column=\"user_id\" property=\"userId\"");
        assertThat(xml).contains("column=\"lottery_type\" property=\"lotteryType\"");
        assertThat(xml).contains("column=\"front_numbers\" property=\"frontNumbers\"");
        assertThat(xml).contains("column=\"back_numbers\" property=\"backNumbers\"");
        assertThat(xml).contains("column=\"favorite_name\" property=\"favoriteName\"");
        assertThat(xml).contains("column=\"remark\" property=\"remark\"");
        assertThat(xml).contains("column=\"status\" property=\"status\"");
        assertThat(xml).contains("column=\"favorite_time\" property=\"favoriteTime\"");
        assertThat(xml).contains("column=\"effective_time\" property=\"effectiveTime\"");
        assertThat(xml).contains("column=\"cancel_time\" property=\"cancelTime\"");
        assertThat(xml).contains("column=\"create_time\" property=\"createTime\"");
        assertThat(xml).contains("column=\"update_time\" property=\"updateTime\"");
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
