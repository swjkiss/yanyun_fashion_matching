<template>
  <view class="mine">
    <view class="mine__status" :style="{ height: statusHeight + 'px' }"></view>

    <!-- 个人信息 -->
    <view class="mine__profile">
      <view class="mine__avatar-wrap">
        <image v-if="avatar" class="mine__avatar" :src="avatar" mode="aspectFill" />
        <text v-else class="mine__avatar-fallback">{{ nickname.slice(0, 1) }}</text>
      </view>
      <view class="mine__profile-main">
        <text class="mine__name">{{ nickname }}</text>
        <view class="mine__identity">
          <view class="mine__identity-line"></view>
          <text class="mine__identity-text">{{ identity }}</text>
        </view>
      </view>
      <view class="mine__seal">
        <text class="mine__seal-text">客</text>
      </view>
    </view>

    <!-- 统计 -->
    <view class="mine__stats">
      <view class="mine__stat" @tap="goOutfits">
        <text class="mine__stat-num">{{ stats.myOutfitCount || 0 }}</text>
        <text class="mine__stat-label">我的搭配</text>
      </view>
      <view class="mine__stat" @tap="goFavorites">
        <text class="mine__stat-num">{{ stats.favoriteCount || 0 }}</text>
        <text class="mine__stat-label">收藏</text>
      </view>
      <view class="mine__stat">
        <text class="mine__stat-num">{{ stats.likeSum || 0 }}</text>
        <text class="mine__stat-label">获赞</text>
      </view>
    </view>

    <!-- 菜单 -->
    <view class="mine__menu">
      <view v-for="m in menus" :key="m.key" class="mine__row u-press" @tap="onMenu(m.key)">
        <text class="mine__row-icon">{{ m.icon }}</text>
        <view class="mine__row-main">
          <text class="mine__row-title">{{ m.title }}</text>
          <text class="mine__row-sub">{{ m.sub }}</text>
        </view>
        <text class="mine__row-arrow">›</text>
      </view>
    </view>

    <view class="mine__footer">
      <view class="u-hairline mine__footer-line"></view>
      <text class="mine__footer-text">燕云十六声 · 时装图鉴</text>
      <text class="mine__footer-ver">v1.0.0</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { resolveImage, statusBarHeight } from '@/config'

const statusHeight = ref(statusBarHeight())
const user = useUserStore()
const stats = ref({ myOutfitCount: 0, favoriteCount: 0, likeSum: 0 })

const nickname = computed(() => user.userInfo?.realname || '青衫客')
const avatar = computed(() => resolveImage(user.userInfo?.avatar))
const identity = computed(() => user.profile?.userInfo?.identity || '江湖游侠')

const menus = [
  { key: 'outfits', icon: '❖', title: '我的搭配', sub: '我留下的衣影' },
  { key: 'favorite', icon: '★', title: '我的收藏', sub: '匣中之物' },
  { key: 'about', icon: '◎', title: '关于', sub: '此间由来' },
  { key: 'clear', icon: '⌫', title: '清空缓存', sub: '搜索历史与本地登录态' },
]

async function loadProfile() {
  try {
    const data = await user.loadProfile()
    stats.value = {
      myOutfitCount: data.myOutfitCount || 0,
      favoriteCount: data.favoriteCount || 0,
      likeSum: data.likeSum || 0,
    }
  } catch (e) {
    /* 静默失败，页面仍可用 */
  }
}

function goOutfits() {
  uni.navigateTo({ url: '/pages/mine/outfits' })
}
function goFavorites() {
  uni.navigateTo({ url: '/pages/mine/favorite' })
}

function onMenu(key) {
  if (key === 'outfits') return goOutfits()
  if (key === 'favorite') return goFavorites()
  if (key === 'about') {
    uni.showModal({
      title: '关于',
      content: '燕云十六声 · 古风时装图鉴与搭配社区。墨为字，纸为底，金为线，朱砂为点。',
      showCancel: false,
      confirmText: '知道了',
    })
    return
  }
  if (key === 'clear') {
    uni.showModal({
      title: '清空缓存',
      content: '将清除搜索历史与本地登录态，确定继续？',
      success: (res) => {
        if (!res.confirm) return
        uni.clearStorageSync()
        user.logout()
        user.ensureLogin().catch(() => {})
        uni.showToast({ title: '已清空', icon: 'none' })
        setTimeout(() => loadProfile(), 400)
      },
    })
  }
}

onShow(() => {
  user.ensureLogin().catch(() => {})
  loadProfile()
})
</script>

<style lang="scss" scoped>
.mine {
  min-height: 100vh;
  background: $paper-100;

  &__status {
    width: 100%;
  }

  &__profile {
    display: flex;
    align-items: center;
    gap: $space-24;
    padding: $space-32;
  }

  &__avatar-wrap {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
    overflow: hidden;
    background: $paper-300;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid rgba(176, 138, 69, 0.4);
  }

  &__avatar {
    width: 100%;
    height: 100%;
  }

  &__avatar-fallback {
    font-family: $font-display;
    font-size: 44rpx;
    color: $ink-500;
  }

  &__profile-main {
    flex: 1;
    min-width: 0;
  }

  &__name {
    display: block;
    font-family: $font-display;
    font-size: 44rpx;
    color: $ink-900;
    letter-spacing: 4rpx;
  }

  &__identity {
    display: flex;
    align-items: center;
    gap: 10rpx;
    margin-top: 8rpx;
  }

  &__identity-line {
    width: 24rpx;
    height: 2rpx;
    background: $gold;
  }

  &__identity-text {
    font-size: 20rpx;
    color: $ink-400;
    letter-spacing: 4rpx;
  }

  &__seal {
    width: 64rpx;
    height: 64rpx;
    border: 1px solid rgba(166, 50, 42, 0.55);
    border-radius: 4rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__seal-text {
    font-family: $font-display;
    font-size: 32rpx;
    color: rgba(166, 50, 42, 0.8);
  }

  &__stats {
    display: flex;
    margin: 0 $space-32;
    padding: $space-28 0;
    background: $paper-50;
    border: 1px solid $ink-100;
    border-radius: $radius-12;
    box-shadow: $shadow-sm;
  }

  &__stat {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6rpx;

    & + & {
      border-left: 1px solid rgba(217, 208, 198, 0.7);
    }
  }

  &__stat-num {
    font-family: $font-display;
    font-size: 40rpx;
    color: $ink-900;
    line-height: 1.1;
  }

  &__stat-label {
    font-size: 20rpx;
    color: $ink-400;
    letter-spacing: 2rpx;
  }

  &__menu {
    margin: $space-32;
    background: $paper-50;
    border: 1px solid $ink-100;
    border-radius: $radius-12;
    overflow: hidden;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: $space-20;
    padding: $space-24 $space-24;

    & + & {
      border-top: 1px solid rgba(217, 208, 198, 0.6);
    }
  }

  &__row-icon {
    width: 44rpx;
    text-align: center;
    font-size: 28rpx;
    color: $gold;
  }

  &__row-main {
    flex: 1;
    min-width: 0;
  }

  &__row-title {
    display: block;
    font-size: $text-28;
    color: $ink-900;
    letter-spacing: 2rpx;
  }

  &__row-sub {
    display: block;
    margin-top: 4rpx;
    font-size: 20rpx;
    color: $ink-300;
  }

  &__row-arrow {
    font-size: 32rpx;
    color: $ink-200;
  }

  &__footer {
    margin-top: $space-56;
    padding: 0 $space-32 $space-48;
    text-align: center;
  }

  &__footer-line {
    margin-bottom: $space-24;
  }

  &__footer-text {
    display: block;
    font-size: 20rpx;
    color: $ink-300;
    letter-spacing: 4rpx;
  }

  &__footer-ver {
    display: block;
    margin-top: 6rpx;
    font-size: 18rpx;
    color: $ink-200;
  }
}
</style>
