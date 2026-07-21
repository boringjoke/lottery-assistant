package com.hotchpotch.lottery.notification.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hotchpotch.lottery.notification.entity.UserNotification;
import com.hotchpotch.lottery.notification.mapper.UserNotificationMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 用户站内通知 Repository。
 */
@Repository
public class UserNotificationRepository {

    private final UserNotificationMapper userNotificationMapper;

    public UserNotificationRepository(UserNotificationMapper userNotificationMapper) {
        this.userNotificationMapper = userNotificationMapper;
    }

    /**
     * 按主键查询通知。
     */
    public Optional<UserNotification> findById(Long id) {
        UserNotification notification = userNotificationMapper.selectById(id);

        return Optional.ofNullable(notification);
    }

    /**
     * 按通知类型和业务幂等键查询通知，用于防止重复创建。
     */
    public Optional<UserNotification> findByNotificationTypeAndBusinessKey(
            String notificationType,
            String businessKey) {
        UserNotification notification = userNotificationMapper.selectOne(Wrappers.<UserNotification>lambdaQuery()
                .eq(UserNotification::getNotificationType, notificationType)
                .eq(UserNotification::getBusinessKey, businessKey));

        return Optional.ofNullable(notification);
    }

    /**
     * 按用户分页查询通知，未读在前，已读在后。
     */
    public List<UserNotification> findPageByUserId(Long userId, int pageNo, int pageSize) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePageNo - 1) * safePageSize;

        return userNotificationMapper.selectPageByUserId(userId, safePageSize, offset);
    }

    /**
     * 按用户统计通知数量。
     */
    public Long countByUserId(Long userId) {
        return userNotificationMapper.selectCount(baseUserQuery(userId));
    }

    /**
     * 按用户和阅读状态统计通知数量。
     */
    public Long countByUserIdAndReadStatus(Long userId, String readStatus) {
        return userNotificationMapper.selectCount(baseUserQuery(userId)
                .eq(UserNotification::getReadStatus, readStatus));
    }

    /**
     * 查询用户全部未读通知，用于批量标记已读。
     */
    public List<UserNotification> findUnreadByUserId(Long userId, String readStatus) {
        return userNotificationMapper.selectList(baseUserQuery(userId)
                .eq(UserNotification::getReadStatus, readStatus)
                .orderByAsc(UserNotification::getCreateTime)
                .orderByAsc(UserNotification::getId));
    }

    /**
     * 插入通知。
     */
    public int insert(UserNotification notification) {
        return userNotificationMapper.insert(notification);
    }

    /**
     * 按主键更新通知。
     */
    public int updateById(UserNotification notification) {
        return userNotificationMapper.updateById(notification);
    }

    /**
     * 构建当前用户通知分页和统计共用查询条件。
     */
    private LambdaQueryWrapper<UserNotification> baseUserQuery(Long userId) {
        return Wrappers.<UserNotification>lambdaQuery()
                .eq(UserNotification::getUserId, userId);
    }
}
