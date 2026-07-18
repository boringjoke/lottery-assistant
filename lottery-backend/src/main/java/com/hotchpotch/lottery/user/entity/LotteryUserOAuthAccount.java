package com.hotchpotch.lottery.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 彩票助手用户第三方账号实体。
 */
@Getter
@Setter
@TableName("lottery_user_oauth_accounts")
public class LotteryUserOAuthAccount {

    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID，由业务代码保证关联有效。 */
    @TableField("user_id")
    private Long userId;

    /** 第三方平台，取值如 WECHAT_MP、WECHAT_WEB、WECHAT_MINI_PROGRAM。 */
    @TableField("provider")
    private String provider;

    /** 第三方平台用户 OpenID。 */
    @TableField("open_id")
    private String openId;

    /** 微信开放平台 UnionID。 */
    @TableField("union_id")
    private String unionId;

    /** 第三方平台昵称。 */
    @TableField("nickname")
    private String nickname;

    /** 第三方平台头像地址。 */
    @TableField("avatar_url")
    private String avatarUrl;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
