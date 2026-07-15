package com.hotchpotch.lottery.draw.record;

import java.util.List;

/**
 * 规范化后的大乐透单注号码。
 *
 * @param frontNumbers 前区号码，升序排列
 * @param backNumbers 后区号码，升序排列
 * @param displayText 两位数字格式化后的展示文本
 */
public record LotteryDltNumber(
        List<Integer> frontNumbers,
        List<Integer> backNumbers,
        String displayText) {
}
