import type { ApiResponse } from '@/types/lottery'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export class ApiError extends Error {
  code: string
  status?: number

  /**
   * 保存后端业务错误码和 HTTP 状态码，方便页面后续按错误类型做差异化展示。
   */
  constructor(message: string, code = 'REQUEST_FAILED', status?: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

interface RequestOptions extends RequestInit {
  query?: Record<string, string | number | boolean | null | undefined>
}

/**
 * 组装请求地址，并过滤空查询参数，避免把未填写的筛选条件传给后端。
 */
function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = API_BASE_URL
    ? new URL(path, API_BASE_URL)
    : new URL(path, window.location.origin)

  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      url.searchParams.set(key, String(value))
    }
  })

  return API_BASE_URL ? url.toString() : `${url.pathname}${url.search}`
}

/**
 * 统一请求入口：处理 HTTP 错误、后端 ApiResponse 解包和业务错误转换。
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { query, headers, body, ...requestOptions } = options
  const response = await fetch(buildUrl(path, query), {
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
    credentials: 'include',
    body,
    ...requestOptions,
  })

  const payload = await readApiResponse<T>(response)
  if (!response.ok) {
    throw new ApiError(
      payload?.message || httpStatusMessage(response.status),
      payload?.code || 'HTTP_STATUS_ERROR',
      response.status,
    )
  }

  if (!payload) {
    throw new ApiError('请求失败，请稍后重试')
  }
  if (!payload.success) {
    throw new ApiError(payload.message || '请求失败，请稍后重试', payload.code)
  }

  return payload.data
}

/**
 * 尝试读取后端统一响应；非 JSON 或空响应时返回 null，由调用方使用兜底文案。
 */
async function readApiResponse<T>(response: Response): Promise<ApiResponse<T> | null> {
  try {
    return (await response.json()) as ApiResponse<T>
  } catch {
    return null
  }
}

/**
 * 将无后端业务响应的 HTTP 状态转换为用户可理解的文案。
 */
function httpStatusMessage(status: number): string {
  if (status === 502 || status === 503 || status === 504) {
    return '后端服务暂不可用，请等待管理员恢复。'
  }

  return `请求失败：${status}`
}

/**
 * 发送 GET 请求，并通过 query 参数拼接查询条件。
 */
export function get<T>(path: string, query?: RequestOptions['query']): Promise<T> {
  return request<T>(path, { method: 'GET', query })
}

/**
 * 发送 JSON POST 请求，适用于号码分析等请求体接口。
 */
export function post<T>(path: string, data?: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    body: JSON.stringify(data ?? {}),
  })
}
