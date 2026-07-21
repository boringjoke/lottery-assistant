package com.hotchpotch.lottery.mail.record;

/**
 * 邮件发送记录分页查询请求。
 *
 * @param pageNo 页码
 * @param pageSize 每页数量
 * @param sendStatus 发送状态筛选，可为空
 */
public record MailSendRecordPageRequest(
        Integer pageNo,
        Integer pageSize,
        String sendStatus) {
}
