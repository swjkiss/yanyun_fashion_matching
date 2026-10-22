<template>
  <div class="login">
    <div class="login__panel">
      <div class="login__brand">
        <span class="login__seal">燕</span>
        <h1 class="login__title">燕云阁</h1>
        <p class="login__sub">时装库管理后台 · JeecgBoot style</p>
      </div>

      <a-form layout="vertical" @finish="onSubmit">
        <a-form-item label="用户名" name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="form.username" size="large" placeholder="admin" allow-clear>
            <template #prefix><user-outlined style="color: var(--text-placeholder)" /></template>
          </a-input>
        </a-form-item>
        <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="form.password" size="large" placeholder="admin123">
            <template #prefix><lock-outlined style="color: var(--text-placeholder)" /></template>
          </a-input-password>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="loading">
            登 录
          </a-button>
        </a-form-item>
      </a-form>

      <div class="login__hint">
        <span class="gold-rule" />
        <p>测试账号：admin / admin123</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import axios from 'axios'
import { useAuthStore } from '../../stores/auth'
import { errText } from '../../utils/format'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })

interface LoginResp {
  success: boolean
  message?: string
  result?: {
    token?: string
    userInfo?: { id: string; username: string; realname?: string; avatar?: string }
  }
}

async function onSubmit() {
  loading.value = true
  try {
    const { data } = await axios.post<LoginResp>('/jeecg-boot/sys/login', {
      username: form.username,
      password: form.password,
    })
    if (data.success && data.result?.token) {
      auth.setLogin(data.result.token, {
        id: data.result.userInfo?.id || '0',
        username: form.username,
        realname: data.result.userInfo?.realname,
        avatar: data.result.userInfo?.avatar,
      })
      message.success('欢迎回来')
      const redirect = (route.query.redirect as string) || '/dashboard'
      router.replace(redirect)
    } else {
      message.error(data.message || '登录失败')
    }
  } catch (e) {
    message.error(errText(e, '登录失败，请检查后端是否启动'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background:
    radial-gradient(1200px 500px at 80% -10%, var(--color-gold-100), transparent),
    radial-gradient(900px 500px at -10% 110%, var(--color-ink-100), transparent),
    var(--bg);
}

.login__panel {
  width: 380px;
  padding: 40px 36px 28px;
  background: var(--surface-raised);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.login__brand {
  text-align: center;
  margin-bottom: 28px;
}

.login__seal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: var(--radius-xs);
  background: var(--accent);
  color: #fdfbf7;
  font-family: var(--font-display);
  font-size: 26px;
  margin-bottom: 12px;
}

.login__title {
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 6px;
  color: var(--text-primary);
  font-weight: var(--weight-bold);
}

.login__sub {
  margin-top: 4px;
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--text-placeholder);
}

.login__hint {
  margin-top: 8px;
  text-align: center;
}

.login__hint .gold-rule {
  margin-bottom: 12px;
  opacity: 0.18;
}

.login__hint p {
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>
