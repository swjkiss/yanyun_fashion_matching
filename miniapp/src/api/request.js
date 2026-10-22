import { API_BASE, API_PREFIX, TOKEN_KEY, USER_KEY } from '@/config'

let redirecting = false

function token() {
  try {
    return uni.getStorageSync(TOKEN_KEY) || ''
  } catch (e) {
    return ''
  }
}

/**
 * 统一请求封装。
 *
 * - 自动带 X-Access-Token
 * - 解 JeecgBoot 的 {success, message, code, result} 信封，直接返回 result
 * - 401 时清理本地登录态并静默重登一次（小程序启动即免密登录，重登通常无感）
 * - 失败默认 toast，调用方传 silent 可自行处理
 *
 * @param {object} options {url, method, data, auth, loading, silent, retry}
 * @returns {Promise<any>}
 */
export function request(options) {
  const {
    url,
    method = 'GET',
    data,
    auth = true,
    loading = false,
    silent = false,
    retry = true,
  } = options || {}

  return new Promise((resolve, reject) => {
    if (loading) uni.showLoading({ title: '加载中', mask: true })
    const header = { 'Content-Type': 'application/json' }
    if (auth && token()) header['X-Access-Token'] = token()

    uni.request({
      url: API_BASE + API_PREFIX + url,
      method,
      data,
      header,
      timeout: 20000,
      success(res) {
        const body = res.data && typeof res.data === 'object' ? res.data : {}
        if (res.statusCode === 200 && body.success) {
          resolve(body.result)
          return
        }
        if (body.code === 401 || res.statusCode === 401) {
          uni.removeStorageSync(TOKEN_KEY)
          uni.removeStorageSync(USER_KEY)
          if (retry && !redirecting) {
            redirecting = true
            // 免密重登后重放本次请求
            import('@/stores/user')
              .then(({ useUserStore }) => useUserStore().ensureLogin(true))
              .then(() => {
                redirecting = false
                return request({ ...options, retry: false })
              })
              .then(resolve)
              .catch((e) => {
                redirecting = false
                reject(e)
              })
            return
          }
          const msg = body.message || '登录已失效'
          if (!silent) uni.showToast({ title: msg, icon: 'none' })
          reject(new Error(msg))
          return
        }
        const msg = body.message || `请求失败（${res.statusCode}）`
        if (!silent) uni.showToast({ title: msg, icon: 'none' })
        reject(new Error(msg))
      },
      fail() {
        const msg = '网络连接失败，请确认服务端已启动'
        if (!silent) uni.showToast({ title: msg, icon: 'none' })
        reject(new Error(msg))
      },
      complete() {
        if (loading) uni.hideLoading()
      },
    })
  })
}

export const http = {
  get: (url, data, opts) => request({ url, method: 'GET', data, ...opts }),
  post: (url, data, opts) => request({ url, method: 'POST', data, ...opts }),
  put: (url, data, opts) => request({ url, method: 'PUT', data, ...opts }),
  del: (url, data, opts) => request({ url, method: 'DELETE', data, ...opts }),
}
