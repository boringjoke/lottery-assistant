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
