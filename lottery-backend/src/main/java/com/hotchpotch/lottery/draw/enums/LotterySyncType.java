package com.hotchpotch.lottery.draw.enums;

/**
 * 开奖同步类型。
 */
public enum LotterySyncType {

    /** 同步最新一期开奖。 */
    LATEST,

    /** 同步一页历史开奖。 */
    HISTORY_PAGE,

    /** 统一历史分页同步。 */
    HISTORY,

    /** 按期号范围同步。 */
    ISSUE_RANGE,

    /** 按开奖日期范围同步。 */
    DATE_RANGE;

    /**
     * 返回对外和数据库存储使用的同步类型编码。
     */
    public String code() {
        return name();
    }
}
