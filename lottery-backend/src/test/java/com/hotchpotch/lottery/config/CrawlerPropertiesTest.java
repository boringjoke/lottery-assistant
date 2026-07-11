package com.hotchpotch.lottery.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class CrawlerPropertiesTest {

    @Test
    void defaultBaseUrlPointsToLocalCrawlerService() {
        CrawlerProperties properties = new CrawlerProperties();

        assertThat(properties.baseUrl()).isEqualTo("http://127.0.0.1:8001");
        assertThat(properties.connectTimeoutMillis()).isEqualTo(3000);
        assertThat(properties.readTimeoutMillis()).isEqualTo(10000);
    }

    @Test
    void bindsCrawlerPropertiesFromConfiguration() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource();
        source.put("lottery.crawler.base-url", "http://crawler:8001");
        source.put("lottery.crawler.connect-timeout-millis", "1500");
        source.put("lottery.crawler.read-timeout-millis", "5000");

        CrawlerProperties properties = new Binder(source)
                .bind("lottery.crawler", CrawlerProperties.class)
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.baseUrl()).isEqualTo("http://crawler:8001");
        assertThat(properties.connectTimeoutMillis()).isEqualTo(1500);
        assertThat(properties.readTimeoutMillis()).isEqualTo(5000);
    }
}
