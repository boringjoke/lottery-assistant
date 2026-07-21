package com.hotchpotch.lottery.mail.record;

import java.util.List;

/**
 * 邮件发送记录分页响应。
 *
 * @param pageNo 当前页码
 * @param pageSize 每页数量
 * @param total 总记录数
 * @param pages 总页数
 * @param sendStatus 发送状态筛选
 * @param records 邮件发送记录列表
 */
public record MailSendRecordPageResponse(
        Integer pageNo,
        Integer pageSize,
        Long total,
        Integer pages,
        String sendStatus,
        List<MailSendRecordResponse> records) {
}
