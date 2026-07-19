import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { logout } from '@/api/auth'
import {
  activateFavorite,
  deactivateFavorite,
  deleteFavorite,
  fetchFavoritePage,
  updateFavorite,
} from '@/api/favorites'
import { fetchUserProfile } from '@/api/user'
import FavoritesView from '../FavoritesView.vue'

const push = vi.fn()

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  useRoute: () => ({ fullPath: '/profile/favorites' }),
  useRouter: () => ({ push }),
}))

vi.mock('@/api/auth', () => ({
  logout: vi.fn(),
}))

vi.mock('@/api/user', () => ({
  fetchUserProfile: vi.fn(),
}))

vi.mock('@/api/favorites', () => ({
  fetchFavoritePage: vi.fn(),
  updateFavorite: vi.fn(),
  deactivateFavorite: vi.fn(),
  activateFavorite: vi.fn(),
  deleteFavorite: vi.fn(),
}))

const profile = {
  userId: 10,
  nickname: '本地普通用户',
  avatarUrl: '/avatars/avatar-01.svg',
  status: 'ACTIVE',
  roles: ['USER'],
  username: 'normal',
  maskedPhone: '138****8000',
  maskedEmail: 'n****l@example.com',
  createTime: '2026-07-18T10:00:00',
  lastLoginTime: '2026-07-18T12:00:00',
}

const activeFavorite = {
  id: 101,
  lotteryType: 'DLT',
  frontNumbers: '01,05,12,23,35',
  backNumbers: '03,11',
  displayText: '01 05 12 23 35 + 03 11',
  favoriteName: '生日组合',
  remark: '家人生日',
  status: 'ACTIVE',
  favoriteTime: '2026-07-18T13:20:00',
  effectiveTime: '2026-07-18T13:20:00',
  cancelTime: null,
}

const cancelledFavorite = {
  ...activeFavorite,
  id: 102,
  favoriteName: '备用号码',
  remark: '',
  status: 'CANCELLED',
  cancelTime: '2026-07-19T09:00:00',
}

function mockFavoritePage(favorites = [activeFavorite], status = 'ACTIVE') {
  vi.mocked(fetchFavoritePage).mockResolvedValue({
    pageNo: 1,
    pageSize: 10,
    total: favorites.length,
    pages: favorites.length ? 1 : 0,
    status,
    keyword: '',
    favorites,
  })
}

describe('FavoritesView', () => {
  beforeEach(() => {
    push.mockReset()
    vi.mocked(fetchUserProfile).mockReset()
    vi.mocked(fetchFavoritePage).mockReset()
    vi.mocked(updateFavorite).mockReset()
    vi.mocked(deactivateFavorite).mockReset()
    vi.mocked(activateFavorite).mockReset()
    vi.mocked(deleteFavorite).mockReset()
    vi.mocked(logout).mockReset()
    vi.mocked(fetchUserProfile).mockResolvedValue(profile)
    mockFavoritePage()
    vi.mocked(updateFavorite).mockResolvedValue(activeFavorite)
    vi.mocked(deactivateFavorite).mockResolvedValue({ ...activeFavorite, status: 'CANCELLED' })
    vi.mocked(activateFavorite).mockResolvedValue({ ...cancelledFavorite, status: 'ACTIVE' })
    vi.mocked(deleteFavorite).mockResolvedValue(undefined)
    vi.mocked(logout).mockResolvedValue(undefined)
    vi.spyOn(window, 'confirm').mockReturnValue(true)
  })

  it('loads current user and renders active favorite list', async () => {
    const wrapper = mount(FavoritesView)
    await flushPromises()

    expect(fetchUserProfile).toHaveBeenCalledOnce()
    expect(fetchFavoritePage).toHaveBeenCalledWith({
      pageNo: 1,
      pageSize: 10,
      status: 'ACTIVE',
      keyword: '',
    })
    expect(wrapper.text()).toContain('我的收藏号码')
    expect(wrapper.text()).toContain('生日组合')
    expect(wrapper.text()).toContain('家人生日')
    expect(wrapper.findAll('.favorite-number-ball--front')).toHaveLength(5)
    expect(wrapper.findAll('.favorite-number-ball--back')).toHaveLength(2)
  })

  it('redirects anonymous visitor to login page', async () => {
    vi.mocked(fetchUserProfile).mockRejectedValue(new Error('请先登录'))

    mount(FavoritesView)
    await flushPromises()

    expect(push).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/profile/favorites' },
    })
    expect(fetchFavoritePage).not.toHaveBeenCalled()
  })

  it('shows analyze entry when active favorites are empty', async () => {
    mockFavoritePage([], 'ACTIVE')

    const wrapper = mount(FavoritesView)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无有效收藏号码')
    expect(wrapper.text()).toContain('去号码分析')
  })

  it('does not show analyze entry when cancelled favorites are empty', async () => {
    mockFavoritePage([], 'CANCELLED')

    const wrapper = mount(FavoritesView)
    await flushPromises()

    await wrapper.find('[data-test="cancelled-filter"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('暂无已取消收藏号码')
    expect(wrapper.text()).not.toContain('去号码分析')
  })

  it('switches to cancelled favorites and supports reactivation', async () => {
    mockFavoritePage([cancelledFavorite], 'CANCELLED')
    const wrapper = mount(FavoritesView)
    await flushPromises()

    await wrapper.find('[data-test="cancelled-filter"]').trigger('click')
    await flushPromises()

    expect(fetchFavoritePage).toHaveBeenLastCalledWith({
      pageNo: 1,
      pageSize: 10,
      status: 'CANCELLED',
      keyword: '',
    })
    expect(wrapper.text()).toContain('备用号码')

    await wrapper.find('[data-test="activate-favorite"]').trigger('click')
    await flushPromises()

    expect(activateFavorite).toHaveBeenCalledWith(102)
    expect(wrapper.text()).toContain('收藏已重新启用')
  })

  it('deactivates active favorite and reloads list', async () => {
    const wrapper = mount(FavoritesView)
    await flushPromises()

    await wrapper.find('[data-test="deactivate-favorite"]').trigger('click')
    await flushPromises()

    expect(deactivateFavorite).toHaveBeenCalledWith(101)
    expect(fetchFavoritePage).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('收藏已取消')
  })

  it('edits favorite name and remark', async () => {
    const wrapper = mount(FavoritesView)
    await flushPromises()

    await wrapper.find('[data-test="edit-favorite"]').trigger('click')
    await wrapper.find('#favoriteName').setValue('新的收藏名')
    await wrapper.find('#favoriteRemark').setValue('新的备注')
    await wrapper.find('.favorites-edit-form').trigger('submit')
    await flushPromises()

    expect(updateFavorite).toHaveBeenCalledWith({
      favoriteId: 101,
      favoriteName: '新的收藏名',
      remark: '新的备注',
    })
    expect(fetchFavoritePage).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('收藏信息已保存')
  })

  it('deletes cancelled favorite after confirmation', async () => {
    mockFavoritePage([cancelledFavorite], 'CANCELLED')
    const wrapper = mount(FavoritesView)
    await flushPromises()

    await wrapper.find('[data-test="cancelled-filter"]').trigger('click')
    await flushPromises()
    await wrapper.find('[data-test="delete-favorite"]').trigger('click')
    await flushPromises()

    expect(window.confirm).toHaveBeenCalledWith('确定删除这条已取消的收藏吗？删除后不可恢复。')
    expect(deleteFavorite).toHaveBeenCalledWith(102)
    expect(fetchFavoritePage).toHaveBeenCalledTimes(3)
    expect(wrapper.text()).toContain('收藏已删除')
  })

  it('does not delete cancelled favorite when confirmation is cancelled', async () => {
    vi.mocked(window.confirm).mockReturnValue(false)
    mockFavoritePage([cancelledFavorite], 'CANCELLED')
    const wrapper = mount(FavoritesView)
    await flushPromises()

    await wrapper.find('[data-test="cancelled-filter"]').trigger('click')
    await flushPromises()
    await wrapper.find('[data-test="delete-favorite"]').trigger('click')
    await flushPromises()

    expect(deleteFavorite).not.toHaveBeenCalled()
  })
})
