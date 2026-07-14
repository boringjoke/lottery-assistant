package com.hotchpotch.lottery.draw.record;

/**
 * 开奖同步任务分页查询请求。
 *
 * @param pageNo 当前页码
 * @param pageSize 每页数量
 * @param status 同步任务状态筛选
 */
public record LotterySyncTaskPageRequest(
        Integer pageNo,
        Integer pageSize,
        String status) {
}
