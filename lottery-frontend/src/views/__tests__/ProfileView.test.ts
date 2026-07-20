import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { logout } from '@/api/auth'
import { fetchUserProfile, updateUserProfile } from '@/api/user'
import ProfileView from '../ProfileView.vue'

const push = vi.fn()

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  useRoute: () => ({ fullPath: '/profile' }),
  useRouter: () => ({ push }),
}))

vi.mock('@/api/auth', () => ({
  logout: vi.fn(),
}))

vi.mock('@/api/user', () => ({
  fetchUserProfile: vi.fn(),
  updateUserProfile: vi.fn(),
}))

const profile = {
  userId: 10,
  nickname: '本地普通用户',
  avatarUrl: null,
  status: 'ACTIVE',
  roles: ['USER'],
  username: 'normal',
  maskedPhone: '138****8000',
  maskedEmail: 'n****l@example.com',
  createTime: '2026-07-18T10:00:00',
  lastLoginTime: '2026-07-18T12:00:00',
}

describe('ProfileView', () => {
  beforeEach(() => {
    push.mockReset()
    vi.mocked(fetchUserProfile).mockReset()
    vi.mocked(updateUserProfile).mockReset()
    vi.mocked(logout).mockReset()
    vi.mocked(fetchUserProfile).mockResolvedValue(profile)
    vi.mocked(updateUserProfile).mockResolvedValue(profile)
    vi.mocked(logout).mockResolvedValue(undefined)
  })

  it('loads and renders current user profile', async () => {
    const wrapper = mount(ProfileView)
    await flushPromises()

    expect(fetchUserProfile).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('个人资料')
    expect(wrapper.text()).toContain('本地普通用户')
    expect(wrapper.text()).toContain('normal')
    expect(wrapper.text()).toContain('138****8000')
    expect(wrapper.text()).toContain('n****l@example.com')
    expect(wrapper.text()).toContain('普通用户')
    expect(wrapper.text()).toContain('2026-07-18 10:00')
    expect(wrapper.text()).toContain('2026-07-18 12:00')
    expect(wrapper.find('#profileNickname').exists()).toBe(true)
    expect(wrapper.findAll('.profile-avatar-option')).toHaveLength(8)
  })

  it('redirects anonymous visitor to login page', async () => {
    vi.mocked(fetchUserProfile).mockRejectedValue(new Error('请先登录'))

    mount(ProfileView)
    await flushPromises()

    expect(push).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/profile' },
    })
  })

  it('shows error message when profile request fails', async () => {
    vi.mocked(fetchUserProfile).mockRejectedValue(new Error('后端服务暂不可用，请等待管理员恢复。'))

    const wrapper = mount(ProfileView)
    await flushPromises()

    expect(wrapper.text()).toContain('后端服务暂不可用，请等待管理员恢复。')
    expect(push).not.toHaveBeenCalled()
  })

  it('supports logout from account menu', async () => {
    const wrapper = mount(ProfileView)
    await flushPromises()

    await wrapper.find('.account-trigger').trigger('click')
    await wrapper.findAll('.account-menu button')[2].trigger('click')
    await flushPromises()

    expect(logout).toHaveBeenCalledOnce()
    expect(push).toHaveBeenCalledWith('/login')
  })

  it('saves nickname and default avatar then refreshes profile state', async () => {
    vi.mocked(updateUserProfile).mockResolvedValue({
      ...profile,
      nickname: '新昵称',
      avatarUrl: '/avatars/avatar-02.svg',
    })
    const wrapper = mount(ProfileView)
    await flushPromises()

    await wrapper.find('#profileNickname').setValue('新昵称')
    await wrapper.findAll('.profile-avatar-option')[1].trigger('click')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(updateUserProfile).toHaveBeenCalledWith({
      nickname: '新昵称',
      avatarUrl: '/avatars/avatar-02.svg',
    })
    expect(wrapper.text()).toContain('个人资料已保存')
    expect(wrapper.find('.account-trigger').text()).toContain('新昵称')
    expect(wrapper.find('.profile-avatar img').attributes('src')).toBe('/avatars/avatar-02.svg')
  })

  it('cancels unsaved profile changes', async () => {
    const wrapper = mount(ProfileView)
    await flushPromises()

    await wrapper.find('#profileNickname').setValue('临时昵称')
    await wrapper.findAll('.profile-avatar-option')[2].trigger('click')
    await wrapper.find('.profile-cancel-button').trigger('click')

    expect((wrapper.find('#profileNickname').element as HTMLInputElement).value).toBe('本地普通用户')
    expect(wrapper.findAll('.profile-avatar-option')[2].classes()).not.toContain('selected')
    expect(updateUserProfile).not.toHaveBeenCalled()
  })

  it('shows backend message when saving profile fails', async () => {
    vi.mocked(updateUserProfile).mockRejectedValue(new Error('头像只能选择系统默认头像'))
    const wrapper = mount(ProfileView)
    await flushPromises()

    await wrapper.find('#profileNickname').setValue('新昵称')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('头像只能选择系统默认头像')
  })
})
