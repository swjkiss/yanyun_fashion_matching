/**
 * 运行期配置。
 *
 * 小程序与 H5 都直连本机 Java 服务端（开发期）。
 * 上线时只需改 API_BASE（或注入环境变量），其余代码不用动。
 */
export const API_BASE = 'http://127.0.0.1:8080'
export const API_PREFIX = '/jeecg-boot'
export const TOKEN_KEY = 'yy_token'
export const USER_KEY = 'yy_user'
export const HISTORY_KEY = 'yy_search_history'

/** 是否是 H5 端（条件编译，构建期即确定）。 */
// #ifdef H5
export const IS_H5 = true
// #endif
// #ifndef H5
export const IS_H5 = false
// #endif

/**
 * 把服务端返回的相对图片路径补成完整 URL。
 * 后端静态资源挂在 context-path 下：/jeecg-boot/images/**
 * @param {string} path 形如 /images/fashion/fashion-01.jpg
 * @returns {string}
 */
export function resolveImage(path) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) {
    // 已是绝对地址：兼容旧的 /images 形态（本机直连开发服务器时补前缀）
    if (/^https?:\/\/127\.0\.0\.1:8080\/images\//.test(path)) {
      return path.replace('/images/', '/jeecg-boot/images/')
    }
    return path
  }
  if (path.indexOf('/jeecg-boot/') === 0) return API_BASE + path
  return API_BASE + '/jeecg-boot' + path
}

/** 状态栏高度（沉浸式导航用）。 */
export function statusBarHeight() {
  try {
    return uni.getSystemInfoSync().statusBarHeight || 0
  } catch (e) {
    return 0
  }
}
