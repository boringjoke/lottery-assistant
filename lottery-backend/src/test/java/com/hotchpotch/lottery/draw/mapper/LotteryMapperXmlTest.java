package com.hotchpotch.lottery.draw.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LotteryMapperXmlTest {

    @Test
    void lotteryDrawMapperHasMatchingXml() {
        assertMapperXml("LotteryDrawMapper.xml", LotteryDrawMapper.class);
    }

    @Test
    void lotteryPrizeTierMapperHasMatchingXml() {
        assertMapperXml("LotteryPrizeTierMapper.xml", LotteryPrizeTierMapper.class);
    }

    @Test
    void lotterySyncTaskMapperHasMatchingXml() {
        assertMapperXml("LotterySyncTaskMapper.xml", LotterySyncTaskMapper.class);
    }

    @Test
    void lotterySyncTaskMapperXmlMapsAsyncProgressColumns() {
        String xml = readResource("mapper/draw/LotterySyncTaskMapper.xml");

        assertThat(xml).contains("column=\"start_page\" property=\"startPage\"");
        assertThat(xml).contains("column=\"current_page\" property=\"currentPage\"");
        assertThat(xml).contains("column=\"last_success_page\" property=\"lastSuccessPage\"");
        assertThat(xml).contains("column=\"failed_page\" property=\"failedPage\"");
        assertThat(xml).contains("column=\"page_size\" property=\"pageSize\"");
        assertThat(xml).contains("column=\"max_pages\" property=\"maxPages\"");
        assertThat(xml).contains("column=\"page_delay_millis\" property=\"pageDelayMillis\"");
        assertThat(xml).contains("column=\"stop_when_last_page\" property=\"stopWhenLastPage\"");
    }

    private void assertMapperXml(String fileName, Class<?> mapperType) {
        String resourcePath = "mapper/draw/" + fileName;
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
