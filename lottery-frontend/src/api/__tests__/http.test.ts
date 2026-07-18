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
})
