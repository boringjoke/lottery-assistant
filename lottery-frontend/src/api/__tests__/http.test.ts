import { describe, expect, it, vi } from 'vitest'

import { ApiError, post } from '@/api/http'

describe('http request', () => {
  it('uses backend ApiResponse message when http status is not ok', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          success: false,
          code: 'INVALID_REQUEST',
          message: '账号和密码不能为空',
          data: null,
        }),
        {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    )

    await expect(post('/api/auth/login', {})).rejects.toMatchObject<ApiError>({
      message: '账号和密码不能为空',
      code: 'INVALID_REQUEST',
      status: 400,
    })

    fetchMock.mockRestore()
  })

  it('uses friendly service unavailable message when proxy returns 502 without ApiResponse', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response('Bad Gateway', {
        status: 502,
        headers: { 'Content-Type': 'text/plain' },
      }),
    )

    await expect(post('/api/draws/dlt', {})).rejects.toMatchObject<ApiError>({
      message: '后端服务暂不可用，请等待管理员恢复。',
      code: 'HTTP_STATUS_ERROR',
      status: 502,
    })

    fetchMock.mockRestore()
  })
})
