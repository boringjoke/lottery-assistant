package com.hotchpotch.lottery.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 彩票助手用户登录凭证实体。
 */
@Getter
@Setter
@TableName("lottery_user_credentials")
public class LotteryUserCredential {

    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID，由业务代码保证关联有效。 */
    @TableField("user_id")
    private Long userId;

    /** 凭证类型，取值如 USERNAME、PHONE、EMAIL。 */
    @TableField("credential_type")
    private String credentialType;

    /** 凭证标识，如用户名、手机号、邮箱。 */
    @TableField("identifier")
    private String identifier;

    /** 密码哈希，验证码登录时可为空。 */
    @TableField("password_hash")
    private String passwordHash;

    /** 手机号或邮箱是否已验证。 */
    @TableField("verified")
    private Boolean verified;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
