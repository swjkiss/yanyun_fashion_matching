<template>
  <div class="layout">
    <!-- 侧栏 -->
    <aside class="layout__sider">
      <div class="layout__brand">
        <span class="layout__seal">燕</span>
        <div class="layout__brand-text">
          <b>燕云阁</b>
          <i>时装库管理后台</i>
        </div>
      </div>
      <nav class="layout__nav">
        <router-link
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="layout__nav-item"
          :class="{ 'is-active': isActive(m.path) }"
        >
          <component :is="m.icon" :size="18" :stroke-width="1.8" />
          <span>{{ m.label }}</span>
        </router-link>
      </nav>
      <div class="layout__sider-foot">
        <span class="layout__ver">v1.0.0</span>
      </div>
    </aside>

    <!-- 主体 -->
    <div class="layout__main">
      <header class="layout__header">
        <div class="layout__crumb">
          <span class="layout__crumb-root">工作台</span>
          <span class="layout__crumb-sep">/</span>
          <span class="layout__crumb-cur">{{ currentTitle }}</span>
        </div>
        <a-dropdown>
          <div class="layout__user">
            <a-avatar :size="30" class="layout__avatar">云</a-avatar>
            <span class="layout__user-name">{{ auth.user?.realname || auth.user?.username }}</span>
            <chevron-down :size="14" :stroke-width="2" />
          </div>
          <template #overlay>
            <a-menu>
              <a-menu-item key="reload" @click="reloadDicts">
                <reload-outlined /> 刷新字典
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout" danger @click="onLogout">
                <logout-outlined /> 退出登录
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </header>
      <main class="layout__content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LayoutDashboard, Shirt, Users, HeartHandshake } from 'lucide-vue-next'
import { LogoutOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menus = [
  { path: '/dashboard', label: '运营总览', icon: h(LayoutDashboard) },
  { path: '/items', label: '时装图鉴', icon: h(Shirt) },
  { path: '/outfits', label: '搭配方案', icon: h(Users) },
  { path: '/favorites', label: '收藏记录', icon: h(HeartHandshake) },
]

const currentTitle = computed(() => (route.meta?.title as string) || '')

function isActive(path: string): boolean {
  return route.path.startsWith(path)
}

function reloadDicts() {
  message.success('字典为内置静态表，无需刷新')
}

function onLogout() {
  auth.logout()
  message.success('已退出')
  router.replace('/login')
}

onMounted(() => {
  document.title = '燕云阁 · 时装库管理后台'
})
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  background: var(--bg);
}

.layout__sider {
  display: flex;
  flex-direction: column;
  width: 216px;
  flex: none;
  background: var(--surface-inverse);
  color: #cfd5dd;
}

.layout__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.layout__seal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-xs);
  background: var(--accent);
  color: #fdfbf7;
  font-family: var(--font-display);
  font-size: 16px;
}

.layout__brand-text b {
  display: block;
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 2px;
  color: #f3ede4;
  font-weight: var(--weight-bold);
}

.layout__brand-text i {
  display: block;
  font-style: normal;
  font-size: 10px;
  letter-spacing: 3px;
  color: rgba(243, 237, 228, 0.45);
  margin-top: 2px;
}

.layout__nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 10px;
}

.layout__nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 12px;
  border-radius: var(--radius-sm);
  color: rgba(243, 237, 228, 0.66);
  font-size: 13px;
  transition: all var(--motion-fast) var(--motion-ease);
}

.layout__nav-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.06);
}

.layout__nav-item.is-active {
  color: #f3ede4;
  background: var(--accent);
  font-weight: 500;
}

.layout__sider-foot {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.layout__ver {
  font-size: 10px;
  letter-spacing: 2px;
  color: rgba(243, 237, 228, 0.3);
}

.layout__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 20px;
  background: var(--surface-raised);
  border-bottom: 1px solid var(--divider);
}

.layout__crumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.layout__crumb-cur {
  color: var(--text-primary);
  font-weight: 500;
}

.layout__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: var(--radius-sm);
}

.layout__user:hover {
  background: var(--color-paper-200);
}

.layout__avatar {
  background: var(--gold);
  font-family: var(--font-display);
  font-size: 13px;
}

.layout__user-name {
  font-size: 13px;
  color: var(--text-primary);
}

.layout__content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 20px;
}
</style>
