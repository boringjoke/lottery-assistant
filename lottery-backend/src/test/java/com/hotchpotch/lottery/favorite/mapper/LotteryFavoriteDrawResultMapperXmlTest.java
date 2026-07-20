package com.hotchpotch.lottery.favorite.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LotteryFavoriteDrawResultMapperXmlTest {

    @Test
    void lotteryFavoriteDrawResultMapperHasMatchingXml() {
        String xml = readResource("mapper/favorite/LotteryFavoriteDrawResultMapper.xml");

        assertThat(xml).contains("namespace=\"" + LotteryFavoriteDrawResultMapper.class.getName() + "\"");
        assertThat(xml).contains("id=\"BaseResultMap\"");
    }

    @Test
    void lotteryFavoriteDrawResultMapperXmlMapsResultColumns() {
        String xml = readResource("mapper/favorite/LotteryFavoriteDrawResultMapper.xml");

        assertThat(xml).contains("column=\"favorite_id\" property=\"favoriteId\"");
        assertThat(xml).contains("column=\"user_id\" property=\"userId\"");
        assertThat(xml).contains("column=\"draw_id\" property=\"drawId\"");
        assertThat(xml).contains("column=\"lottery_type\" property=\"lotteryType\"");
        assertThat(xml).contains("column=\"issue_no\" property=\"issueNo\"");
        assertThat(xml).contains("column=\"draw_date\" property=\"drawDate\"");
        assertThat(xml).contains("column=\"favorite_front_numbers\" property=\"favoriteFrontNumbers\"");
        assertThat(xml).contains("column=\"favorite_back_numbers\" property=\"favoriteBackNumbers\"");
        assertThat(xml).contains("column=\"draw_front_numbers\" property=\"drawFrontNumbers\"");
        assertThat(xml).contains("column=\"draw_back_numbers\" property=\"drawBackNumbers\"");
        assertThat(xml).contains("column=\"front_hit_count\" property=\"frontHitCount\"");
        assertThat(xml).contains("column=\"back_hit_count\" property=\"backHitCount\"");
        assertThat(xml).contains("column=\"winning\" property=\"winning\"");
        assertThat(xml).contains("column=\"prize_level\" property=\"prizeLevel\"");
        assertThat(xml).contains("column=\"prize_name\" property=\"prizeName\"");
        assertThat(xml).contains("column=\"rule_version\" property=\"ruleVersion\"");
        assertThat(xml).contains("column=\"stake_amount\" property=\"stakeAmount\"");
        assertThat(xml).contains("column=\"calculated_time\" property=\"calculatedTime\"");
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
