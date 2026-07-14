package com.hotchpotch.lottery.crawler.record;

import java.util.List;

/**
 * crawler 历史开奖分页响应。
 *
 * @param pageNo 当前页码
 * @param pageSize 每页数量
 * @param pages 总页数
 * @param total 总记录数
 * @param draws 开奖数据列表
 */
public record CrawlerHistoryPageResponse(
        Integer pageNo,
        Integer pageSize,
        Integer pages,
        Integer total,
        List<CrawlerDraw> draws) {
}
