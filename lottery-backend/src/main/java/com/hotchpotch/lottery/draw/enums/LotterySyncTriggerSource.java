package com.hotchpotch.lottery.draw.enums;

/**
 * 开奖同步任务触发来源。
 */
public enum LotterySyncTriggerSource {

    /** 管理端手动触发。 */
    ADMIN,

    /** 定时任务触发。 */
    SCHEDULED,

    /** 系统自动触发。 */
    SYSTEM;

    /**
     * 返回对外和数据库存储使用的触发来源编码。
     */
    public String code() {
        return name();
    }
}
