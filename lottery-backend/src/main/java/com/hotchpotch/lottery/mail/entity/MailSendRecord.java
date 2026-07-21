package com.hotchpotch.lottery.mail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 邮件发送记录实体。
 */
@Getter
@Setter
@TableName("mail_send_records")
public class MailSendRecord {

    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID，由业务代码保证关联有效。 */
    @TableField("user_id")
    private Long userId;

    /** 业务类型，如 LOTTERY_FAVORITE_WINNING。 */
    @TableField("business_type")
    private String businessType;

    /** 业务键，如 DLT:26076:FAVORITE:101。 */
    @TableField("business_key")
    private String businessKey;

    /** 发件邮箱。 */
    @TableField("from_email")
    private String fromEmail;

    /** 收件邮箱。 */
    @TableField("to_email")
    private String toEmail;

    /** 邮件标题。 */
    @TableField("subject")
    private String subject;

    /** 邮件正文。 */
    @TableField("content")
    private String content;

    /** 发送状态，取值如 PENDING、SUCCESS、FAILED。 */
    @TableField("send_status")
    private String sendStatus;

    /** 失败原因。 */
    @TableField("error_message")
    private String errorMessage;

    /** 发送尝试次数。 */
    @TableField("attempt_count")
    private Integer attemptCount;

    /** 发送成功时间。 */
    @TableField("sent_time")
    private LocalDateTime sentTime;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
