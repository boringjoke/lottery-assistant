package com.hotchpotch.lottery.draw.enums;

/**
 * 开奖同步任务状态。
 */
public enum LotterySyncTaskStatus {

    /** 等待后台执行。 */
    PENDING,

    /** 正在执行。 */
    RUNNING,

    /** 执行成功。 */
    SUCCESS,

    /** 部分成功。 */
    PARTIAL_SUCCESS,

    /** 执行失败。 */
    FAILED,

    /** 已发起重试。 */
    RETRIED;

    /**
     * 返回对外和数据库存储使用的状态编码。
     */
    public String code() {
        return name();
    }
}
