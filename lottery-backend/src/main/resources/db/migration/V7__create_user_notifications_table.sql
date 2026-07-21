CREATE TABLE user_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID，由业务代码保证关联有效',
    notification_type VARCHAR(64) NOT NULL COMMENT '通知类型，如 FAVORITE_WINNING',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型，如 LOTTERY_FAVORITE_WINNING',
    business_key VARCHAR(128) NOT NULL COMMENT '业务幂等键，如 DLT:26076:FAVORITE:101',
    title VARCHAR(128) NOT NULL COMMENT '通知标题',
    content VARCHAR(512) NOT NULL COMMENT '通知内容',
    read_status VARCHAR(32) NOT NULL DEFAULT 'UNREAD' COMMENT '阅读状态，UNREAD 未读，READ 已读',
    read_time DATETIME NULL COMMENT '阅读时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_business (notification_type, business_key),
    KEY idx_user_read_time (user_id, read_status, create_time),
    KEY idx_user_create_time (user_id, create_time),
    KEY idx_business_key (business_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户站内通知表';
