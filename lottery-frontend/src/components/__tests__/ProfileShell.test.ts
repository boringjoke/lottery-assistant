import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchUnreadNotificationCount } from '@/api/notifications'
import ProfileShell from '@/components/profile/ProfileShell.vue'

const push = vi.fn()

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  useRoute: () => ({ fullPath: '/profile' }),
  useRouter: () => ({ push }),
}))

vi.mock('@/api/notifications', () => ({
  fetchUnreadNotificationCount: vi.fn(),
}))

describe('ProfileShell', () => {
  beforeEach(() => {
    push.mockReset()
    vi.mocked(fetchUnreadNotificationCount).mockReset()
    vi.mocked(fetchUnreadNotificationCount).mockResolvedValue(0)
  })

  it('renders brand, navigation and slot content', () => {
    const wrapper = mount(ProfileShell, {
      props: {
        currentUser: {
          userId: 10,
          nickname: '本地普通用户',
          avatarUrl: null,
          roles: ['USER'],
        },
        activeNav: 'profile',
      },
      slots: {
        default: '<section class="shell-slot-content">业务内容</section>',
      },
    })

    expect(wrapper.text()).toContain('彩票助手')
    expect(wrapper.text()).toContain('个人中心')
    expect(wrapper.text()).toContain('个人资料')
    expect(wrapper.text()).toContain('我的收藏')
    expect(wrapper.text()).toContain('我的通知')
    expect(wrapper.text()).toContain('业务内容')
    expect(wrapper.find('.profile-shell-nav__item.active').text()).toContain('个人资料')
  })

  it('highlights favorites navigation', () => {
    const wrapper = mount(ProfileShell, {
      props: {
        currentUser: null,
        activeNav: 'favorites',
      },
    })

    expect(wrapper.find('.profile-shell-nav__item.active').text()).toContain('我的收藏')
  })

  it('highlights notifications navigation', () => {
    const wrapper = mount(ProfileShell, {
      props: {
        currentUser: null,
        activeNav: 'notifications',
      },
    })

    expect(wrapper.find('.profile-shell-nav__item.active').text()).toContain('我的通知')
  })

  it('emits logout from account menu', async () => {
    const wrapper = mount(ProfileShell, {
      props: {
        currentUser: {
          userId: 10,
          nickname: '本地普通用户',
          avatarUrl: null,
          roles: ['USER'],
        },
        activeNav: 'profile',
      },
    })
    await flushPromises()

    await wrapper.find('.account-trigger').trigger('click')
    await wrapper.findAll('.account-menu button')[3].trigger('click')

    expect(wrapper.emitted('logout')).toHaveLength(1)
  })
})
