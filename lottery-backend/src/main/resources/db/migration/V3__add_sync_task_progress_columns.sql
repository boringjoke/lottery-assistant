ALTER TABLE lottery_sync_tasks
    ADD COLUMN start_page INT NULL COMMENT '历史同步起始页码' AFTER request_params,
    ADD COLUMN current_page INT NULL COMMENT '当前同步页码' AFTER start_page,
    ADD COLUMN last_success_page INT NULL COMMENT '最后成功同步页码' AFTER current_page,
    ADD COLUMN failed_page INT NULL COMMENT '失败页码' AFTER last_success_page,
    ADD COLUMN page_size INT NULL COMMENT '每页数量' AFTER failed_page,
    ADD COLUMN max_pages INT NULL COMMENT '最大同步页数' AFTER page_size,
    ADD COLUMN page_delay_millis INT NULL COMMENT '每页间隔毫秒' AFTER max_pages,
    ADD COLUMN stop_when_last_page TINYINT(1) NOT NULL DEFAULT 1 COMMENT '遇到最后一页时是否停止' AFTER page_delay_millis;
