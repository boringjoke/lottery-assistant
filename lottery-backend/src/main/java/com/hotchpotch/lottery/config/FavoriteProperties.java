package com.hotchpotch.lottery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用户收藏号码配置。
 */
@ConfigurationProperties(prefix = "lottery.favorite")
public class FavoriteProperties {

    /**
     * 单个用户最多保留的有效收藏数量。
     */
    private int maxActiveCount = 100;

    public int maxActiveCount() {
        return maxActiveCount;
    }

    public void setMaxActiveCount(int maxActiveCount) {
        this.maxActiveCount = maxActiveCount;
    }
}
