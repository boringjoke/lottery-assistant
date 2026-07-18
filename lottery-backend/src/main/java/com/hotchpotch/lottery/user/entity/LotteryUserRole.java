package com.hotchpotch.lottery.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 彩票助手用户角色实体。
 */
@Getter
@Setter
@TableName("lottery_user_roles")
public class LotteryUserRole {

    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID，由业务代码保证关联有效。 */
    @TableField("user_id")
    private Long userId;

    /** 角色编码，取值如 USER、ADMIN。 */
    @TableField("role_code")
    private String roleCode;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
