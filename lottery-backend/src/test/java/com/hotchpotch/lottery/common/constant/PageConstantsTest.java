package com.hotchpotch.lottery.common.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageConstantsTest {

    /**
     * 验证分页默认值与公开接口约定保持一致。
     */
    @Test
    void pageConstantsMatchPublicContract() {
        assertThat(PageConstants.DEFAULT_PAGE_NO).isEqualTo(1);
        assertThat(PageConstants.DEFAULT_PAGE_SIZE).isEqualTo(20);
        assertThat(PageConstants.MAX_PAGE_SIZE).isEqualTo(100);
        assertThat(PageConstants.DEFAULT_PAGE_NO_TEXT).isEqualTo("1");
        assertThat(PageConstants.DEFAULT_PAGE_SIZE_TEXT).isEqualTo("20");
    }
}
