package com.hotchpotch.lottery.mail.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hotchpotch.lottery.mail.entity.MailSendRecord;
import com.hotchpotch.lottery.mail.mapper.MailSendRecordMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 邮件发送记录 Repository。
 */
@Repository
public class MailSendRecordRepository {

    private final MailSendRecordMapper mailSendRecordMapper;

    public MailSendRecordRepository(MailSendRecordMapper mailSendRecordMapper) {
        this.mailSendRecordMapper = mailSendRecordMapper;
    }

    /**
     * 按主键查询邮件发送记录。
     */
    public Optional<MailSendRecord> findById(Long id) {
        MailSendRecord record = mailSendRecordMapper.selectById(id);

        return Optional.ofNullable(record);
    }

    /**
     * 按业务类型和业务键查询邮件发送记录。
     */
    public Optional<MailSendRecord> findByBusinessTypeAndBusinessKey(String businessType, String businessKey) {
        MailSendRecord record = mailSendRecordMapper.selectOne(Wrappers.<MailSendRecord>lambdaQuery()
                .eq(MailSendRecord::getBusinessType, businessType)
                .eq(MailSendRecord::getBusinessKey, businessKey));

        return Optional.ofNullable(record);
    }

    /**
     * 分页查询邮件发送记录，可按发送状态筛选，按创建时间倒序返回。
     */
    public List<MailSendRecord> findPage(String sendStatus, int pageNo, int pageSize) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePageNo - 1) * safePageSize;

        return mailSendRecordMapper.selectList(baseQuery(sendStatus)
                .orderByDesc(MailSendRecord::getCreateTime)
                .orderByDesc(MailSendRecord::getId)
                .last("LIMIT " + safePageSize + " OFFSET " + offset));
    }

    /**
     * 统计邮件发送记录数量，可按发送状态筛选。
     */
    public Long count(String sendStatus) {
        return mailSendRecordMapper.selectCount(baseQuery(sendStatus));
    }

    /**
     * 插入邮件发送记录。
     */
    public int insert(MailSendRecord record) {
        return mailSendRecordMapper.insert(record);
    }

    /**
     * 按主键更新邮件发送记录。
     */
    public int updateById(MailSendRecord record) {
        return mailSendRecordMapper.updateById(record);
    }

    private LambdaQueryWrapper<MailSendRecord> baseQuery(String sendStatus) {
        LambdaQueryWrapper<MailSendRecord> queryWrapper = Wrappers.<MailSendRecord>lambdaQuery();
        if (sendStatus != null && !sendStatus.isBlank()) {
            queryWrapper.eq(MailSendRecord::getSendStatus, sendStatus.trim());
        }

        return queryWrapper;
    }
}
