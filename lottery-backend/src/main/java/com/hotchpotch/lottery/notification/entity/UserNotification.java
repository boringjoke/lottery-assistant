package com.hotchpotch.lottery.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户站内通知实体。
 */
@Getter
@Setter
@TableName("user_notifications")
public class UserNotification {

    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID，由业务代码保证关联有效。 */
    @TableField("user_id")
    private Long userId;

    /** 通知类型，如 FAVORITE_WINNING。 */
    @TableField("notification_type")
    private String notificationType;

    /** 业务类型，如 LOTTERY_FAVORITE_WINNING。 */
    @TableField("business_type")
    private String businessType;

    /** 业务幂等键，如 DLT:26076:FAVORITE:101。 */
    @TableField("business_key")
    private String businessKey;

    /** 通知标题。 */
    @TableField("title")
    private String title;

    /** 通知内容。 */
    @TableField("content")
    private String content;

    /** 阅读状态，取值如 UNREAD、READ。 */
    @TableField("read_status")
    private String readStatus;

    /** 阅读时间。 */
    @TableField("read_time")
    private LocalDateTime readTime;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
