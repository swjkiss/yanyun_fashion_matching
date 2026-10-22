import axios from 'axios'
import { message } from 'ant-design-vue'
import { useAuthStore } from '../stores/auth'
import { errText } from '../utils/format'

/** JeecgBoot 风格统一响应 */
export interface ApiResult<T = unknown> {
  success: boolean
  message?: string
  code?: number | string
  result?: T
  timestamp?: number
}

export const http = axios.create({
  baseURL: import.meta.env.DEV ? '' : 'http://127.0.0.1:8080',
  timeout: 15000,
})

/** 拼上 /jeecg-boot 前缀（dev 走代理，prod 直连） */
function url(path: string): string {
  const suffix = path.startsWith('/') ? path : `/${path}`
  return `${import.meta.env.DEV ? '/jeecg-boot' : '/jeecg-boot'}${suffix}`
}

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) config.headers['X-Access-Token'] = auth.token
  return config
})

http.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResult
    // 文件等二进制响应直接放行
    if (response.config.responseType === 'blob' || data?.success === undefined) return response
    if (!data.success) {
      // 401 / 令牌失效 → 回登录
      if (String(data.code) === '401') {
        const auth = useAuthStore()
        auth.logout()
        if (!location.pathname.startsWith('/login')) {
          location.href = `/login?redirect=${encodeURIComponent(location.pathname)}`
        }
      }
      return Promise.reject(new Error(data.message || '操作失败'))
    }
    return response
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      const auth = useAuthStore()
      auth.logout()
      if (!location.pathname.startsWith('/login')) {
        location.href = `/login?redirect=${encodeURIComponent(location.pathname)}`
      }
    }
    return Promise.reject(new Error(errText(error)))
  }
)

/** 统一 GET，返回 result 部分 */
export async function apiGet<T = unknown>(path: string, params?: Record<string, unknown>): Promise<T> {
  const res = await http.get(url(path), { params })
  return (res.data as ApiResult<T>).result as T
}

/** 统一 POST / PUT / DELETE */
export async function apiPost<T = unknown>(path: string, body?: unknown): Promise<T> {
  const res = await http.post(url(path), body ?? {})
  return (res.data as ApiResult<T>).result as T
}
export async function apiPut<T = unknown>(path: string, body?: unknown): Promise<T> {
  const res = await http.put(url(path), body ?? {})
  return (res.data as ApiResult<T>).result as T
}
export async function apiDelete<T = unknown>(path: string, params?: Record<string, unknown>): Promise<T> {
  const res = await http.delete(url(path), { params })
  return (res.data as ApiResult<T>).result as T
}

/** 表单上传（multipart） */
export async function apiUpload(file: File): Promise<{ url: string; fileName: string }> {
  const form = new FormData()
  form.append('file', file)
  const res = await http.post(url('/sys/common/upload'), form)
  return (res.data as ApiResult<{ url: string; fileName: string }>).result as {
    url: string
    fileName: string
  }
}

/** 分页加载错误统一 toast（列表页 catch 后自行处理） */
export function toastError(error: unknown) {
  message.error(errText(error))
}
