import { http } from './request'

/** 首页聚合（banner / stats / categories / recommend / hotOutfits）。 */
export const fetchHome = () => http.get('/app/home')

/** 免密登录。 */
export const guestLogin = (username) =>
  http.post('/app/guest-login', { username }, { auth: false, silent: true })

/** 时装检索。 */
export const fetchItems = (params) => http.get('/app/fashion/items', params)

/** 时装详情。 */
export const fetchItemDetail = (id) => http.get(`/app/fashion/item/${id}`)

/** 筛选面板选项。 */
export const fetchFilters = () => http.get('/app/fashion/filters')

/** 搭配广场。 */
export const fetchOutfits = (params) => http.get('/app/outfits', params)

/** 搭配详情。 */
export const fetchOutfitDetail = (id) => http.get(`/app/outfit/${id}`)

/** 创建搭配。 */
export const createOutfit = (form) => http.post('/app/outfit', form)

/** 点赞。 */
export const likeOutfit = (id) => http.post(`/app/outfit/${id}/like`)

/** 收藏 / 取消收藏。 */
export const toggleFavorite = (outfitId) => http.post('/app/favorite/toggle', { outfitId })

/** 我的收藏。 */
export const fetchFavorites = (params) => http.get('/app/favorites', params)

/** 我的搭配。 */
export const fetchMyOutfits = (params) => http.get('/app/my/outfits', params)

/** 我的资料与统计。 */
export const fetchProfile = () => http.get('/app/user/profile')

/** 数据字典。 */
export const fetchDict = (code) => http.get(`/sys/dict/getDictItems/${code}`, null, { auth: false })
