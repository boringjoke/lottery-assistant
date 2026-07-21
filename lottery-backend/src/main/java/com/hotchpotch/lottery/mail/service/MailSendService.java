package com.hotchpotch.lottery.mail.service;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.config.MailProperties;
import com.hotchpotch.lottery.mail.entity.MailSendRecord;
import com.hotchpotch.lottery.mail.enums.MailSendStatus;
import com.hotchpotch.lottery.mail.record.MailSendRequest;
import com.hotchpotch.lottery.mail.repository.MailSendRecordRepository;
import java.time.LocalDateTime;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务。
 */
@Service
public class MailSendService {

    private static final int EMAIL_MAX_LENGTH = 128;
    private static final int SUBJECT_MAX_LENGTH = 128;
    private static final int CONTENT_MAX_LENGTH = 2048;
    private static final int BUSINESS_TYPE_MAX_LENGTH = 64;
    private static final int BUSINESS_KEY_MAX_LENGTH = 128;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1024;

    private final MailProperties mailProperties;
    private final JavaMailSender javaMailSender;
    private final MailSendRecordRepository mailSendRecordRepository;

    public MailSendService(
            MailProperties mailProperties,
            JavaMailSender javaMailSender,
            MailSendRecordRepository mailSendRecordRepository) {
        this.mailProperties = mailProperties;
        this.javaMailSender = javaMailSender;
        this.mailSendRecordRepository = mailSendRecordRepository;
    }

    /**
     * 发送普通文本邮件，并同步更新邮件发送记录。
     *
     * <p>这个方法有意不把 SMTP 配置缺失或发送失败继续向外抛出，
     * 而是落库为 FAILED 后返回记录。这样后续挂到开奖同步链路时，
     * 邮件失败不会影响开奖和站内通知入库。</p>
     */
    public MailSendRecord sendText(MailSendRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "邮件发送请求不能为空");
        }

        String toEmail = requireText(request.toEmail(), "收件邮箱不能为空", EMAIL_MAX_LENGTH);
        String subject = requireText(request.subject(), "邮件标题不能为空", SUBJECT_MAX_LENGTH);
        String content = requireText(request.content(), "邮件正文不能为空", CONTENT_MAX_LENGTH);
        String businessType = trimToNull(request.businessType(), BUSINESS_TYPE_MAX_LENGTH, "业务类型不能超过 64 个字符");
        String businessKey = trimToNull(request.businessKey(), BUSINESS_KEY_MAX_LENGTH, "业务键不能超过 128 个字符");
        String fromEmail = resolveFromEmail();

        // 先准备发送记录，再做配置检查和真实发送，确保失败也有记录可查。
        MailSendRecord record = prepareRecord(
                request.userId(),
                businessType,
                businessKey,
                fromEmail,
                toEmail,
                subject,
                content);

        // 同一业务邮件已经成功发送时保持幂等，避免重复打扰用户。
        if (MailSendStatus.SUCCESS.code().equals(record.getSendStatus())) {
            return record;
        }
        if (!mailProperties.enabled()) {
            return markFailed(record, "邮件发送未启用");
        }
        if (fromEmail.isBlank()) {
            return markFailed(record, "发件邮箱未配置");
        }
        if (mailProperties.host().isBlank()) {
            return markFailed(record, "SMTP 服务器未配置");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);
            javaMailSender.send(message);

            return markSuccess(record);
        } catch (MailException ex) {
            // SMTP 认证失败、连接超时、被服务商拒信等都统一记录为发送失败。
            return markFailed(record, ex.getMessage());
        }
    }

    /**
     * 创建或复用邮件发送记录。
     *
     * <p>有 businessType + businessKey 的业务邮件会复用同一条记录；
     * 之前失败的记录会重置为 PENDING 后重试，之前成功的记录直接返回。</p>
     */
    private MailSendRecord prepareRecord(
            Long userId,
            String businessType,
            String businessKey,
            String fromEmail,
            String toEmail,
            String subject,
            String content) {
        MailSendRecord existingRecord = findExistingBusinessRecord(businessType, businessKey);
        if (existingRecord != null) {
            if (MailSendStatus.SUCCESS.code().equals(existingRecord.getSendStatus())) {
                return existingRecord;
            }

            // 失败重试时允许更新收件人、标题和正文，保留同一条业务记录。
            existingRecord.setUserId(userId);
            existingRecord.setFromEmail(fromEmail);
            existingRecord.setToEmail(toEmail);
            existingRecord.setSubject(subject);
            existingRecord.setContent(content);
            existingRecord.setSendStatus(MailSendStatus.PENDING.code());
            existingRecord.setErrorMessage(null);
            existingRecord.setSentTime(null);
            existingRecord.setUpdateTime(LocalDateTime.now());
            mailSendRecordRepository.updateById(existingRecord);

            return existingRecord;
        }

        LocalDateTime now = LocalDateTime.now();
        MailSendRecord record = new MailSendRecord();
        record.setUserId(userId);
        record.setBusinessType(businessType);
        record.setBusinessKey(businessKey);
        record.setFromEmail(fromEmail);
        record.setToEmail(toEmail);
        record.setSubject(subject);
        record.setContent(content);
        record.setSendStatus(MailSendStatus.PENDING.code());
        record.setErrorMessage(null);
        record.setAttemptCount(0);
        record.setSentTime(null);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        mailSendRecordRepository.insert(record);

        return record;
    }

    /**
     * 查询已有业务邮件记录；缺少业务类型或业务键时视为普通邮件，不做幂等复用。
     */
    private MailSendRecord findExistingBusinessRecord(String businessType, String businessKey) {
        if (businessType == null || businessKey == null) {
            return null;
        }

        return mailSendRecordRepository
                .findByBusinessTypeAndBusinessKey(businessType, businessKey)
                .orElse(null);
    }

    /**
     * 标记发送成功，并记录成功时间。
     */
    private MailSendRecord markSuccess(MailSendRecord record) {
        LocalDateTime now = LocalDateTime.now();
        record.setSendStatus(MailSendStatus.SUCCESS.code());
        record.setErrorMessage(null);
        record.setAttemptCount(nextAttemptCount(record));
        record.setSentTime(now);
        record.setUpdateTime(now);
        mailSendRecordRepository.updateById(record);

        return record;
    }

    /**
     * 标记发送失败，并把失败原因截断后保存，避免异常堆栈过长撑爆字段。
     */
    private MailSendRecord markFailed(MailSendRecord record, String errorMessage) {
        record.setSendStatus(MailSendStatus.FAILED.code());
        record.setErrorMessage(truncateErrorMessage(errorMessage));
        record.setAttemptCount(nextAttemptCount(record));
        record.setSentTime(null);
        record.setUpdateTime(LocalDateTime.now());
        mailSendRecordRepository.updateById(record);

        return record;
    }

    /**
     * 计算下一次发送尝试次数；兼容历史数据里 attempt_count 为空的情况。
     */
    private int nextAttemptCount(MailSendRecord record) {
        return (record.getAttemptCount() == null ? 0 : record.getAttemptCount()) + 1;
    }

    /**
     * 获取发件邮箱；优先使用显式 from，未配置时回退到 SMTP username。
     */
    private String resolveFromEmail() {
        String fromEmail = trimToNull(mailProperties.from(), EMAIL_MAX_LENGTH, "发件邮箱不能超过 128 个字符");
        if (fromEmail != null) {
            return fromEmail;
        }

        String username = trimToNull(mailProperties.username(), EMAIL_MAX_LENGTH, "SMTP 账号不能超过 128 个字符");
        return username == null ? "" : username;
    }

    /**
     * 校验必填文本并统一去除前后空白。
     */
    private String requireText(String value, String errorMessage, int maxLength) {
        String normalizedValue = trimToNull(value, maxLength, errorMessage.replace("不能为空", "不能超过 " + maxLength + " 个字符"));
        if (normalizedValue == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, errorMessage);
        }

        return normalizedValue;
    }

    /**
     * 去除文本前后空白；空字符串转为 null，并校验最大长度。
     */
    private String trimToNull(String value, int maxLength, String maxLengthErrorMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.trim();
        if (normalizedValue.length() > maxLength) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, maxLengthErrorMessage);
        }

        return normalizedValue;
    }

    /**
     * 规范化失败原因，兜底生成可读文案，并控制入库长度。
     */
    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "邮件发送失败";
        }

        String normalizedErrorMessage = errorMessage.trim();
        if (normalizedErrorMessage.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return normalizedErrorMessage;
        }

        return normalizedErrorMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}
