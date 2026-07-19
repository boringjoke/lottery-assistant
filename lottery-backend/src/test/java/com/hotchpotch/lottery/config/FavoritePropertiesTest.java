package com.hotchpotch.lottery.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class FavoritePropertiesTest {

    /**
     * 验证单个用户默认最多保留 100 条有效收藏。
     */
    @Test
    void defaultFavoritePropertiesUseOneHundredActiveFavorites() {
        FavoriteProperties properties = new FavoriteProperties();

        assertThat(properties.maxActiveCount()).isEqualTo(100);
    }

    /**
     * 验证收藏配置可以从 application.yaml 或环境变量绑定。
     */
    @Test
    void bindsFavoritePropertiesFromConfiguration() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource();
        source.put("lottery.favorite.max-active-count", "50");

        FavoriteProperties properties = new Binder(source)
                .bind("lottery.favorite", FavoriteProperties.class)
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.maxActiveCount()).isEqualTo(50);
    }
}
