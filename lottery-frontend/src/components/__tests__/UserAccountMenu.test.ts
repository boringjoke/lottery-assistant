import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchUnreadNotificationCount } from '@/api/notifications'
import UserAccountMenu from '@/components/UserAccountMenu.vue'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ fullPath: '/lottery-assistant?tab=overview' }),
  useRouter: () => ({ push }),
}))

vi.mock('@/api/notifications', () => ({
  fetchUnreadNotificationCount: vi.fn(),
}))

describe('UserAccountMenu', () => {
  beforeEach(() => {
    push.mockReset()
    vi.mocked(fetchUnreadNotificationCount).mockReset()
    vi.mocked(fetchUnreadNotificationCount).mockResolvedValue(0)
  })

  it('shows login entry for guest and keeps redirect', async () => {
    const wrapper = mount(UserAccountMenu, {
      props: {
        user: null,
      },
    })

    await wrapper.find('.login-entry-button').trigger('click')

    expect(wrapper.find('.login-entry-button').text()).toBe('登录')
    expect(wrapper.find('.login-entry-button__icon-default').exists()).toBe(true)
    expect(wrapper.find('.login-entry-button__icon-hover').exists()).toBe(true)
    expect(push).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/lottery-assistant?tab=overview' },
    })
  })

  it('shows profile and logout for normal user', async () => {
    vi.mocked(fetchUnreadNotificationCount).mockResolvedValue(3)
    const wrapper = mount(UserAccountMenu, {
      props: {
        user: {
          userId: 1,
          nickname: '普通用户',
          avatarUrl: null,
          roles: ['USER'],
        },
      },
    })
    await flushPromises()

    await wrapper.find('.account-trigger').trigger('click')

    expect(wrapper.text()).toContain('普通用户')
    expect(wrapper.text()).toContain('个人中心')
    expect(wrapper.text()).toContain('我的收藏')
    expect(wrapper.text()).toContain('我的通知')
    expect(wrapper.find('.account-trigger__badge').text()).toBe('3')
    expect(wrapper.text()).toContain('退出登录')
    expect(wrapper.text()).not.toContain('数据同步管理')
    expect(fetchUnreadNotificationCount).toHaveBeenCalledOnce()

    await wrapper.findAll('.account-menu button')[0].trigger('click')

    expect(push).toHaveBeenCalledWith('/profile')

    await wrapper.find('.account-trigger').trigger('click')
    await wrapper.findAll('.account-menu button')[1].trigger('click')

    expect(push).toHaveBeenCalledWith('/profile/favorites')

    await wrapper.find('.account-trigger').trigger('click')
    await wrapper.findAll('.account-menu button')[2].trigger('click')

    expect(push).toHaveBeenCalledWith('/profile/notifications')
  })

  it('shows admin sync entry for admin user', async () => {
    const wrapper = mount(UserAccountMenu, {
      props: {
        user: {
          userId: 1,
          nickname: '管理员',
          avatarUrl: null,
          roles: ['USER', 'ADMIN'],
        },
      },
    })
    await flushPromises()

    await wrapper.find('.account-trigger').trigger('click')

    expect(wrapper.text()).toContain('我的收藏')
    expect(wrapper.text()).toContain('我的通知')
    expect(wrapper.text()).toContain('数据同步管理')

    await wrapper.findAll('.account-menu button')[3].trigger('click')

    expect(push).toHaveBeenCalledWith('/admin/lottery-sync')
  })

  it('uses external unread count when provided', async () => {
    const wrapper = mount(UserAccountMenu, {
      props: {
        user: {
          userId: 1,
          nickname: '普通用户',
          avatarUrl: null,
          roles: ['USER'],
        },
        notificationUnreadCount: 4,
      },
    })

    expect(fetchUnreadNotificationCount).not.toHaveBeenCalled()
    expect(wrapper.find('.account-trigger__badge').text()).toBe('4')

    await wrapper.setProps({ notificationUnreadCount: 0 })

    expect(wrapper.find('.account-trigger__badge').exists()).toBe(false)
  })

  it('emits logout from dropdown', async () => {
    const wrapper = mount(UserAccountMenu, {
      props: {
        user: {
          userId: 1,
          nickname: '管理员',
          avatarUrl: null,
          roles: ['USER', 'ADMIN'],
        },
      },
    })
    await flushPromises()

    await wrapper.find('.account-trigger').trigger('click')
    await wrapper.findAll('.account-menu button')[4].trigger('click')

    expect(wrapper.emitted('logout')).toHaveLength(1)
    expect(wrapper.find('.account-menu').exists()).toBe(false)
  })
})
