package com.hotchpotch.lottery.crawler.record;

import java.util.List;

/**
 * crawler 历史开奖分页响应。
 */
public record CrawlerHistoryPageResponse(
        Integer pageNo,
        Integer pageSize,
        Integer pages,
        Integer total,
        List<CrawlerDraw> draws) {
}
