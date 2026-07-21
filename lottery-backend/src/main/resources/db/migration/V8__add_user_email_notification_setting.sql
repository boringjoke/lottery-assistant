ALTER TABLE lottery_users
    ADD COLUMN email_notification_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开启邮箱通知' AFTER status,
    ADD KEY idx_lottery_users_email_notification_enabled (email_notification_enabled);
