package com.hotchpotch.lottery.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 彩票助手用户实体。
 */
@Getter
@Setter
@TableName("lottery_users")
public class LotteryUser {

    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户昵称。 */
    @TableField("nickname")
    private String nickname;

    /** 用户头像地址。 */
    @TableField("avatar_url")
    private String avatarUrl;

    /** 用户状态，取值如 ACTIVE、DISABLED。 */
    @TableField("status")
    private String status;

    /** 最近登录时间。 */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
