package com.hotchpotch.lottery.draw.record;

import java.util.List;

/**
 * 开奖同步任务分页响应数据。
 *
 * @param pageNo 当前页码
 * @param pageSize 每页数量
 * @param total 总记录数
 * @param pages 总页数
 * @param status 同步任务状态筛选
 * @param tasks 同步任务列表
 */
public record LotterySyncTaskPageResponse(
        Integer pageNo,
        Integer pageSize,
        Long total,
        Integer pages,
        String status,
        List<LotterySyncTaskResponse> tasks) {
}
