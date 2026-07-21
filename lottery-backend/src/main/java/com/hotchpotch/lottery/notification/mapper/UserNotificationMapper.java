package com.hotchpotch.lottery.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotchpotch.lottery.notification.entity.UserNotification;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 用户站内通知 Mapper。
 */
public interface UserNotificationMapper extends BaseMapper<UserNotification> {

    /**
     * 分页查询用户通知：未读在前，已读在后；同组内按更新时间倒序。
     */
    List<UserNotification> selectPageByUserId(
            @Param("userId") Long userId,
            @Param("limit") int limit,
            @Param("offset") int offset);
}
