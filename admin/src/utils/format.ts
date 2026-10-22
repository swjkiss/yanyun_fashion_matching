/** 通用格式化 / 小工具。 */

/**
 * 图片相对路径 → 可直接用于 <img src> 的地址。
 * 后端静态资源挂在 /jeecg-boot/images/**，vite 代理已转发 /jeecg-boot 与 /images，
 * 存量数据（/images/...）与本地上传（/jeecg-boot/images/... 两种形态）都归一到代理路径。
 */
export function imgUrl(path?: string | null): string {
  if (!path) return ''
  if (/^(https?:)?\/\//i.test(path) || path.startsWith('data:')) return path
  const p = path.startsWith('/') ? path : `/${path}`
  if (p.startsWith('/jeecg-boot/')) return p.replace(/^\/jeecg-boot\/images\//, '/jeecg-boot/images/')
  return p.startsWith('/images/') ? `/jeecg-boot${p}` : p
}

/** 从任意异常中提取可展示的中文提示 */
export function errText(error: unknown, fallback = '请求失败，请稍后重试'): string {
  if (typeof error === 'string' && error) return error
  if (error && typeof error === 'object') {
    const candidate = error as { message?: unknown; msg?: unknown }
    if (typeof candidate.message === 'string' && candidate.message) return candidate.message
    if (typeof candidate.msg === 'string' && candidate.msg) return candidate.msg
  }
  return fallback
}

/** 数字千分位 + 万级缩写 */
export function formatNumber(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '0'
  if (Math.abs(value) >= 10000) {
    const wan = value / 10000
    return `${wan >= 10 ? wan.toFixed(0) : wan.toFixed(1)}万`
  }
  return value.toLocaleString('zh-CN')
}

/** 原样千分位 */
export function formatCount(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '0'
  return value.toLocaleString('zh-CN')
}

/** 时间：只保留 yyyy-MM-dd HH:mm */
export function formatDateTime(value?: string | null): string {
  if (!value) return '-'
  return value.length >= 16 ? value.slice(0, 16) : value
}

/** 时间：只保留 yyyy-MM-dd */
export function formatDate(value?: string | null): string {
  if (!value) return '-'
  return value.slice(0, 10)
}

/** 逗号分隔标签字符串 → 数组 */
export function splitTags(value?: string | null): string[] {
  if (!value) return []
  return value
    .split(/[,，、]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

/** 数组 → 逗号分隔字符串 */
export function joinTags(tags: string[]): string {
  return tags.join(',')
}

/** 字符串截断 */
export function truncate(value?: string | null, length = 28): string {
  if (!value) return ''
  return value.length > length ? `${value.slice(0, length)}…` : value
}

/** 延迟（用于输入防抖 / 最小 loading 时长） */
export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/** 简易防抖 */
export function debounce<A extends unknown[]>(fn: (...args: A) => void, wait = 320) {
  let timer: ReturnType<typeof setTimeout> | undefined
  return (...args: A) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn(...args), wait)
  }
}
