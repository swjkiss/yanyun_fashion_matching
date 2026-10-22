import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('./views/login/index.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('./layouts/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: () => import('./views/dashboard/index.vue'), meta: { title: '总览' } },
      { path: 'items', name: 'items', component: () => import('./views/items/index.vue'), meta: { title: '时装图鉴' } },
      { path: 'outfits', name: 'outfits', component: () => import('./views/outfits/index.vue'), meta: { title: '搭配方案' } },
      { path: 'favorites', name: 'favorites', component: () => import('./views/favorites/index.vue'), meta: { title: '收藏记录' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  document.title = (to.meta.title ? `${to.meta.title} · ` : '') + '燕云阁 · 时装库管理后台'
  return true
})
