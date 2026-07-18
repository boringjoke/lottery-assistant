import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchCurrentUser, logout } from '@/api/auth'
import LotteryAssistantView from '../LotteryAssistantView.vue'

const push = vi.fn()
const replace = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { tab: 'overview' } }),
  useRouter: () => ({ push, replace }),
}))

vi.mock('@/api/auth', () => ({
  fetchCurrentUser: vi.fn(),
  logout: vi.fn(),
}))

function mountView() {
  return mount(LotteryAssistantView, {
    global: {
      stubs: {
        OverviewTab: true,
        HistoryTab: true,
        AnalyzeTab: true,
        DrawDetailDialog: true,
      },
    },
  })
}

describe('LotteryAssistantView', () => {
  beforeEach(() => {
    push.mockReset()
    replace.mockReset()
    vi.mocked(fetchCurrentUser).mockReset()
    vi.mocked(logout).mockReset()
    vi.mocked(fetchCurrentUser).mockRejectedValue(new Error('请先登录'))
  })

  it('shows login entry and navigates to login page', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('.login-entry-button').trigger('click')

    expect(wrapper.find('.login-entry-button').text()).toBe('登录')
    expect(wrapper.find('.login-entry-button__icon').exists()).toBe(true)
    expect(wrapper.find('.header-divider').exists()).toBe(true)
    expect(wrapper.find('.header-actions').text()).toMatch(/票种[\s\S]*大乐透[\s\S]*登录/)
    expect(push).toHaveBeenCalledWith('/login')
  })

  it('shows current user and supports logout after authenticated', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue({
      userId: 1,
      nickname: '管理员',
      avatarUrl: null,
      roles: ['USER', 'ADMIN'],
    })
    vi.mocked(logout).mockResolvedValue(undefined)
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.find('.logout-button').exists()).toBe(true)

    await wrapper.find('.logout-button').trigger('click')
    await flushPromises()

    expect(logout).toHaveBeenCalledOnce()
    expect(wrapper.find('.login-entry-button').text()).toBe('登录')
  })
})
