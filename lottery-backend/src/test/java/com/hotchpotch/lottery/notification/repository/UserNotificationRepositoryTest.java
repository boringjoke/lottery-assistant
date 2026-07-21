package com.hotchpotch.lottery.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotchpotch.lottery.notification.entity.UserNotification;
import com.hotchpotch.lottery.notification.mapper.UserNotificationMapper;
import org.junit.jupiter.api.Test;

class UserNotificationRepositoryTest {

    @Test
    void mapperExtendsMybatisPlusBaseMapper() {
        assertThat(BaseMapper.class).isAssignableFrom(UserNotificationMapper.class);
    }

    @Test
    void repositoryFindsNotificationById() {
        UserNotificationMapper mapper = mock(UserNotificationMapper.class);
        UserNotification notification = new UserNotification();
        when(mapper.selectById(10L)).thenReturn(notification);

        UserNotificationRepository repository = new UserNotificationRepository(mapper);

        assertThat(repository.findById(10L)).containsSame(notification);
        verify(mapper).selectById(10L);
    }

    @Test
    void repositoryFindsNotificationByBusinessKey() {
        UserNotificationMapper mapper = mock(UserNotificationMapper.class);
        UserNotification notification = new UserNotification();
        when(mapper.selectOne(anyNotificationWrapper())).thenReturn(notification);

        UserNotificationRepository repository = new UserNotificationRepository(mapper);

        assertThat(repository.findByNotificationTypeAndBusinessKey("FAVORITE_WINNING", "DLT:26076:FAVORITE:101"))
                .containsSame(notification);
        verify(mapper).selectOne(anyNotificationWrapper());
    }

    @Test
    void repositoryFindsPageAndCountsByUser() {
        UserNotificationMapper mapper = mock(UserNotificationMapper.class);
        UserNotification notification = new UserNotification();
        when(mapper.selectPageByUserId(10L, 20, 0)).thenReturn(java.util.List.of(notification));
        when(mapper.selectCount(anyNotificationWrapper())).thenReturn(1L);

        UserNotificationRepository repository = new UserNotificationRepository(mapper);

        assertThat(repository.findPageByUserId(10L, 1, 20)).containsExactly(notification);
        assertThat(repository.countByUserId(10L)).isEqualTo(1L);
        verify(mapper).selectPageByUserId(10L, 20, 0);
        verify(mapper).selectCount(anyNotificationWrapper());
    }

    @Test
    void repositoryNormalizesPageBeforeFindingNotifications() {
        UserNotificationMapper mapper = mock(UserNotificationMapper.class);
        UserNotification notification = new UserNotification();
        when(mapper.selectPageByUserId(10L, 1, 0)).thenReturn(java.util.List.of(notification));

        UserNotificationRepository repository = new UserNotificationRepository(mapper);

        assertThat(repository.findPageByUserId(10L, 0, 0)).containsExactly(notification);
        verify(mapper).selectPageByUserId(10L, 1, 0);
    }

    @Test
    void repositoryCountsByUserAndReadStatus() {
        UserNotificationMapper mapper = mock(UserNotificationMapper.class);
        when(mapper.selectCount(anyNotificationWrapper())).thenReturn(3L);

        UserNotificationRepository repository = new UserNotificationRepository(mapper);

        assertThat(repository.countByUserIdAndReadStatus(10L, "UNREAD")).isEqualTo(3L);
        verify(mapper).selectCount(anyNotificationWrapper());
    }

    @Test
    void repositoryFindsUnreadNotificationsByUser() {
        UserNotificationMapper mapper = mock(UserNotificationMapper.class);
        UserNotification notification = new UserNotification();
        when(mapper.selectList(anyNotificationWrapper())).thenReturn(java.util.List.of(notification));

        UserNotificationRepository repository = new UserNotificationRepository(mapper);

        assertThat(repository.findUnreadByUserId(10L, "UNREAD")).containsExactly(notification);
        verify(mapper).selectList(anyNotificationWrapper());
    }

    @Test
    void repositoryDelegatesInsertAndUpdateById() {
        UserNotificationMapper mapper = mock(UserNotificationMapper.class);
        UserNotification notification = new UserNotification();
        when(mapper.insert(notification)).thenReturn(1);
        when(mapper.updateById(notification)).thenReturn(1);

        UserNotificationRepository repository = new UserNotificationRepository(mapper);

        assertThat(repository.insert(notification)).isEqualTo(1);
        assertThat(repository.updateById(notification)).isEqualTo(1);
        verify(mapper).insert(notification);
        verify(mapper).updateById(notification);
    }

    private Wrapper<UserNotification> anyNotificationWrapper() {
        return any();
    }
}
