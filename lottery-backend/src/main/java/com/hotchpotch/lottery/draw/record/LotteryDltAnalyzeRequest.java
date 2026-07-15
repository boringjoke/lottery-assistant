package com.hotchpotch.lottery.draw.record;

import java.util.List;

/**
 * 大乐透号码分析请求。
 *
 * @param numbers 待分析号码列表，每个元素表示一注号码
 */
public record LotteryDltAnalyzeRequest(List<String> numbers) {
}
