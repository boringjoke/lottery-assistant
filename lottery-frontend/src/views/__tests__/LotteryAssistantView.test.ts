import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import LotteryAssistantView from '../LotteryAssistantView.vue'

const push = vi.fn()
const replace = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { tab: 'overview' } }),
  useRouter: () => ({ push, replace }),
}))

describe('LotteryAssistantView', () => {
  it('shows login entry and navigates to login page', async () => {
    push.mockReset()
    const wrapper = mount(LotteryAssistantView, {
      global: {
        stubs: {
          OverviewTab: true,
          HistoryTab: true,
          AnalyzeTab: true,
          DrawDetailDialog: true,
        },
      },
    })

    await wrapper.find('.login-entry-button').trigger('click')

    expect(wrapper.find('.login-entry-button').text()).toBe('登录')
    expect(wrapper.find('.login-entry-button__icon').exists()).toBe(true)
    expect(wrapper.find('.header-divider').exists()).toBe(true)
    expect(wrapper.find('.header-actions').text()).toMatch(/票种[\s\S]*大乐透[\s\S]*登录/)
    expect(push).toHaveBeenCalledWith('/login')
  })
})
