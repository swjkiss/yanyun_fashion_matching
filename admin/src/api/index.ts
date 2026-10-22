import { apiGet, apiPost, apiPut, apiDelete } from './client'
import { builtinDicts, dictText, rarityVars, colorFamilyVars, genderOptions, itemStatusOptions, outfitStatusOptions, outfitStatusMeta, rarityClass, rarityColor, colorFamilyColor, dictItem, dictText as _dt } from '../utils/dict'

export * from './client'
export {
  builtinDicts,
  dictText,
  rarityVars,
  colorFamilyVars,
  genderOptions,
  itemStatusOptions,
  outfitStatusOptions,
  outfitStatusMeta,
  rarityClass,
  rarityColor,
  colorFamilyColor,
  dictItem,
  _dt,
}

/* ============ 时装 ============ */
export interface FashionItem {
  id: string
  name: string
  code: string
  glyph?: string
  category: string
  rarity: string
  colorFamily?: string
  style?: string
  series?: string
  gender: string
  imageUrl?: string
  summary?: string
  description?: string
  story?: string
  tags?: string
  obtainMethod?: string
  obtainDetail?: string
  releaseDate?: string
  status: number
  sort: number
  viewCount: number
  likeCount: number
  isRecommend: number
  category_dictText?: string
  rarity_dictText?: string
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

export interface PageData<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export const itemApi = {
  list: (params: Record<string, unknown>) => apiGet<PageData<FashionItem>>('/fashion/item/list', params),
  detail: (id: string) => apiGet<FashionItem>('/fashion/item/queryById', { id }),
  add: (body: Record<string, unknown>) => apiPost<FashionItem>('/fashion/item/add', body),
  edit: (body: Record<string, unknown>) => apiPut('/fashion/item/edit', body),
  remove: (id: string) => apiDelete('/fashion/item/delete', { id }),
  removeBatch: (ids: string[]) => apiDelete('/fashion/item/deleteBatch', { ids: ids.join(',') }),
  status: (id: string, status: number) => apiPut('/fashion/item/status', { id, status }),
}

/* ============ 搭配 ============ */
export interface OutfitRow {
  id: string
  title: string
  subtitle?: string
  description?: string
  authorId?: string
  authorName?: string
  authorAvatar?: string
  coverImage?: string
  itemCount: number
  styleTag?: string
  sceneTag?: string
  colorTone?: string
  status: number
  viewCount: number
  likeCount: number
  favCount: number
  createTime?: string
  updateTime?: string
  [key: string]: unknown
}

export const outfitApi = {
  list: (params: Record<string, unknown>) => apiGet<PageData<OutfitRow>>('/fashion/outfit/list', params),
  detail: (id: string) => apiGet<{ outfit: OutfitRow; items: FashionItem[] }>('/fashion/outfit/queryById', { id }),
  add: (body: Record<string, unknown>) => apiPost('/fashion/outfit/add', body),
  edit: (body: Record<string, unknown>) => apiPut('/fashion/outfit/edit', body),
  remove: (id: string) => apiDelete('/fashion/outfit/delete', { id }),
  removeBatch: (ids: string[]) => apiDelete('/fashion/outfit/deleteBatch', { ids: ids.join(',') }),
  audit: (id: string, status: number) => apiPut('/fashion/outfit/audit', { id, status }),
}

/* ============ 收藏 & 统计 ============ */
export interface FavoriteRow {
  id: string
  outfitId?: string
  outfitTitle?: string
  coverImage?: string
  userId?: string
  userName?: string
  createTime?: string
}

export const favoriteApi = {
  list: (params: Record<string, unknown>) => apiGet<PageData<FavoriteRow>>('/fashion/favorite/list', params),
  remove: (id: string) => apiDelete('/fashion/favorite/delete', { id }),
}

export interface Overview {
  itemTotal: number
  itemOnline: number
  outfitTotal: number
  outfitPending: number
  favoriteTotal: number
  viewTotal: number
  categoryDist: { name: string; value: number }[]
  rarityDist: { name: string; value: number }[]
  colorDist: { name: string; value: number }[]
  topOutfits: { id: string; title: string; favCount: number; coverImage: string }[]
}

export const statsApi = {
  overview: () => apiGet<Overview>('/fashion/stats/overview'),
  trend: () => apiGet<{ date: string; items: number; outfits: number; favorites: number }[]>('/fashion/stats/trend'),
}
