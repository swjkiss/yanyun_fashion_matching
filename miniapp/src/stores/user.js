import { defineStore } from 'pinia'
import { guestLogin, fetchProfile } from '@/api'
import { TOKEN_KEY, USER_KEY } from '@/config'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null,
    profile: null,
    logging: false,
  }),

  getters: {
    logged: (state) => !!state.token,
    nickname: (state) => state.userInfo?.realname || state.userInfo?.username || '未登录',
    avatar: (state) => state.userInfo?.avatar || '',
    identity: (state) => state.profile?.userInfo?.identity || '江湖游侠',
  },

  actions: {
    restore() {
      try {
        this.token = uni.getStorageSync(TOKEN_KEY) || ''
        const cached = uni.getStorageSync(USER_KEY)
        this.userInfo = cached ? (typeof cached === 'string' ? JSON.parse(cached) : cached) : null
      } catch (e) {
        this.token = ''
        this.userInfo = null
      }
    },

    /**
     * 确保有登录态。小程序启动即免密登录，用户无感知。
     * @param {boolean} force 强制重新登录（token 失效时用）
     */
    async ensureLogin(force = false) {
      if (!force && this.token) return this.token
      if (this.logging) return this.token
      this.logging = true
      try {
        const result = await guestLogin('demo')
        this.token = result.token
        this.userInfo = result.userInfo
        uni.setStorageSync(TOKEN_KEY, result.token)
        uni.setStorageSync(USER_KEY, JSON.stringify(result.userInfo))
        return this.token
      } finally {
        this.logging = false
      }
    },

    async loadProfile() {
      await this.ensureLogin()
      const data = await fetchProfile()
      this.profile = data
      if (data.userInfo) this.userInfo = { ...this.userInfo, ...data.userInfo }
      return data
    },

    logout() {
      this.token = ''
      this.userInfo = null
      this.profile = null
      uni.removeStorageSync(TOKEN_KEY)
      uni.removeStorageSync(USER_KEY)
    },
  },
})
