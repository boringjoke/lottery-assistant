package com.hotchpotch.lottery.draw.enums;

/**
 * 彩票类型。
 */
public enum LotteryType {

    /** 大乐透。 */
    DLT;

    /**
     * 返回对外和数据库存储使用的彩票类型编码。
     */
    public String code() {
        return name();
    }
}
