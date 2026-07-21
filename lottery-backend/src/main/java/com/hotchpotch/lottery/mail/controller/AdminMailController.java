package com.hotchpotch.lottery.mail.controller;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.common.response.ApiResponse;
import com.hotchpotch.lottery.common.constant.PageConstants;
import com.hotchpotch.lottery.mail.entity.MailSendRecord;
import com.hotchpotch.lottery.mail.record.AdminMailSendTestRequest;
import com.hotchpotch.lottery.mail.record.MailSendRecordPageRequest;
import com.hotchpotch.lottery.mail.record.MailSendRecordPageResponse;
import com.hotchpotch.lottery.mail.record.MailSendRecordResponse;
import com.hotchpotch.lottery.mail.record.MailSendRequest;
import com.hotchpotch.lottery.mail.repository.MailSendRecordRepository;
import com.hotchpotch.lottery.mail.service.MailSendService;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端邮件接口。
 */
@RestController
@RequestMapping("/api/admin/mail")
public class AdminMailController {

    private final MailSendService mailSendService;
    private final MailSendRecordRepository mailSendRecordRepository;

    public AdminMailController(
            MailSendService mailSendService,
            MailSendRecordRepository mailSendRecordRepository) {
        this.mailSendService = mailSendService;
        this.mailSendRecordRepository = mailSendRecordRepository;
    }

    /**
     * 发送测试邮件，用于本地或服务器验证 SMTP 配置是否可用。
     */
    @PostMapping("/sendTest")
    public ApiResponse<MailSendRecordResponse> sendTestMail(
            @RequestBody(required = false) AdminMailSendTestRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "测试邮件请求体不能为空");
        }

        MailSendRecord record = mailSendService.sendText(new MailSendRequest(
                null,
                null,
                null,
                request.toEmail(),
                request.subject(),
                request.content()));

        return ApiResponse.success(toResponse(record));
    }

    /**
     * 分页查询邮件发送记录；仅用于当前版本通过接口查看发送结果。
     */
    @PostMapping("/records")
    public ApiResponse<MailSendRecordPageResponse> listMailSendRecords(
            @RequestBody(required = false) MailSendRecordPageRequest request) {
        int pageNo = defaultIfNull(request == null ? null : request.pageNo(), PageConstants.DEFAULT_PAGE_NO);
        int pageSize = Math.min(
                defaultIfNull(request == null ? null : request.pageSize(), PageConstants.DEFAULT_PAGE_SIZE),
                PageConstants.MAX_PAGE_SIZE);
        String sendStatus = normalizeSendStatus(request == null ? null : request.sendStatus());
        long total = mailSendRecordRepository.count(sendStatus);
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        List<MailSendRecordResponse> records = mailSendRecordRepository
                .findPage(sendStatus, pageNo, pageSize)
                .stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.success(new MailSendRecordPageResponse(
                pageNo,
                pageSize,
                total,
                pages,
                sendStatus,
                records));
    }

    private MailSendRecordResponse toResponse(MailSendRecord record) {
        return new MailSendRecordResponse(
                record.getId(),
                record.getUserId(),
                record.getBusinessType(),
                record.getBusinessKey(),
                record.getFromEmail(),
                record.getToEmail(),
                record.getSubject(),
                record.getSendStatus(),
                record.getErrorMessage(),
                record.getAttemptCount(),
                record.getSentTime(),
                record.getCreateTime(),
                record.getUpdateTime());
    }

    private int defaultIfNull(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String normalizeSendStatus(String sendStatus) {
        if (sendStatus == null || sendStatus.isBlank()) {
            return null;
        }

        return sendStatus.trim();
    }
}
