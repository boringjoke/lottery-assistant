import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import AppToast from '../AppToast.vue'

describe('AppToast', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders message with status role by default', () => {
    const wrapper = mount(AppToast, {
      props: {
        message: '收藏已取消',
        type: 'success',
      },
    })

    expect(wrapper.text()).toContain('收藏已取消')
    expect(wrapper.find('.app-toast').attributes('role')).toBe('status')
    expect(wrapper.find('.app-toast').classes()).toContain('app-toast--success')
  })

  it('emits close after duration', async () => {
    const wrapper = mount(AppToast, {
      props: {
        message: '收藏已重新启用',
        duration: 1200,
      },
    })

    await vi.advanceTimersByTimeAsync(1200)

    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('emits close when close button is clicked', async () => {
    const wrapper = mount(AppToast, {
      props: {
        message: '收藏信息已保存',
        duration: 0,
      },
    })

    await wrapper.find('.app-toast__close').trigger('click')

    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('uses alert role for error message', () => {
    const wrapper = mount(AppToast, {
      props: {
        message: '操作失败',
        type: 'error',
      },
    })

    expect(wrapper.find('.app-toast').attributes('role')).toBe('alert')
  })
})
