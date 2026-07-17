import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import LoginView from '../LoginView.vue'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push }),
}))

describe('LoginView', () => {
  it('returns to the public assistant overview page', async () => {
    push.mockReset()
    const wrapper = mount(LoginView)

    await wrapper.find('.back-button').trigger('click')

    expect(push).toHaveBeenCalledWith('/lottery-assistant?tab=overview')
  })

  it('renders static login form with account helper links', () => {
    const wrapper = mount(LoginView)

    expect(wrapper.text()).toContain('欢迎回来')
    expect(wrapper.text()).toContain('用户名')
    expect(wrapper.text()).toContain('密码')
    expect(wrapper.text()).toContain('注册账号')
    expect(wrapper.text()).toContain('忘记密码？')
  })
})
