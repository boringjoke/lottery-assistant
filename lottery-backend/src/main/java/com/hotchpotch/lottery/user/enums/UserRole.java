package com.hotchpotch.lottery.user.enums;

/**
 * 用户角色。
 */
public enum UserRole {

    /** 普通用户。 */
    USER,

    /** 管理员。 */
    ADMIN;

    /**
     * 返回数据库存储和权限判断使用的用户角色编码。
     */
    public String code() {
        return name();
    }
}
