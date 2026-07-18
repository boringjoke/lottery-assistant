import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { loginWithPassword } from '@/api/auth'
import LoginView from '../LoginView.vue'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push }),
}))

vi.mock('@/api/auth', () => ({
  loginWithPassword: vi.fn(),
}))

describe('LoginView', () => {
  beforeEach(() => {
    push.mockReset()
    vi.mocked(loginWithPassword).mockReset()
  })

  it('returns to the public assistant overview page', async () => {
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

  it('submits account and password then returns to overview page', async () => {
    vi.mocked(loginWithPassword).mockResolvedValue({
      token: 'opaque-token',
      userId: 1,
      nickname: '管理员',
      avatarUrl: null,
      roles: ['USER', 'ADMIN'],
      expireTime: '2026-07-25T10:00:00',
    })
    const wrapper = mount(LoginView)

    await wrapper.find('#username').setValue('admin')
    await wrapper.find('#password').setValue('secret')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(loginWithPassword).toHaveBeenCalledWith({ account: 'admin', password: 'secret' })
    expect(push).toHaveBeenCalledWith('/lottery-assistant?tab=overview')
  })

  it('shows required message when submitting empty form', async () => {
    const wrapper = mount(LoginView)

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('账号和密码不能为空')
    expect(loginWithPassword).not.toHaveBeenCalled()
    expect(push).not.toHaveBeenCalled()
  })

  it('clears username and password independently', async () => {
    const wrapper = mount(LoginView)

    await wrapper.find('#username').setValue('admin')
    await wrapper.find('#password').setValue('secret')

    await wrapper.find('[data-testid="clear-username"]').trigger('click')
    expect((wrapper.find('#username').element as HTMLInputElement).value).toBe('')
    expect((wrapper.find('#password').element as HTMLInputElement).value).toBe('secret')

    await wrapper.find('[data-testid="clear-password"]').trigger('click')
    expect((wrapper.find('#password').element as HTMLInputElement).value).toBe('')
  })

  it('shows login error without leaving current page', async () => {
    vi.mocked(loginWithPassword).mockRejectedValue(new Error('账号或密码错误'))
    const wrapper = mount(LoginView)

    await wrapper.find('#username').setValue('admin')
    await wrapper.find('#password').setValue('wrong')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('账号或密码错误')
    expect(push).not.toHaveBeenCalled()
  })
})
