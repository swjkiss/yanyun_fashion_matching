import { defineStore } from 'pinia'

const TOKEN_KEY = 'yy_admin_token'
const USER_KEY = 'yy_admin_user'

export interface AdminUser {
  id: string
  username: string
  realname?: string
  avatar?: string
}

function readUser(): AdminUser | null {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('admin-auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser(),
  }),
  actions: {
    setLogin(token: string, user: AdminUser) {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
