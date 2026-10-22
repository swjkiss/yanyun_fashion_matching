<template>
  <view class="nav" :class="{ 'nav--transparent': transparent }">
    <view class="nav__status" :style="{ height: statusHeight + 'px' }"></view>
    <view class="nav__bar">
      <view v-if="showBack" class="nav__back u-press" @tap="goBack">
        <text class="nav__back-icon">‹</text>
      </view>
      <view class="nav__title-wrap">
        <text class="nav__title">{{ title }}</text>
        <text v-if="subtitle" class="nav__subtitle">{{ subtitle }}</text>
      </view>
      <view class="nav__right">
        <slot name="right"></slot>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { statusBarHeight } from '@/config'

const props = defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  showBack: { type: Boolean, default: false },
  transparent: { type: Boolean, default: false },
})

const statusHeight = ref(statusBarHeight())

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.nav {
  position: relative;
  z-index: 20;
  background: $paper-100;

  &--transparent {
    background: transparent;
  }

  &__bar {
    height: 88rpx;
    display: flex;
    align-items: center;
    padding: 0 $space-32;
  }

  &__back {
    width: 56rpx;
    height: 56rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-left: -12rpx;
  }

  &__back-icon {
    font-size: 56rpx;
    line-height: 1;
    color: $ink-700;
    margin-top: -6rpx;
  }

  &__title-wrap {
    flex: 1;
    display: flex;
    align-items: baseline;
    gap: $space-12;
    min-width: 0;
  }

  &__title {
    font-family: $font-display;
    font-size: $text-34;
    letter-spacing: $tracking-wide;
    color: $ink-900;
    font-weight: $weight-medium;
  }

  &__subtitle {
    font-size: $text-22;
    color: $ink-400;
    letter-spacing: $tracking-wider;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: $space-16;
  }
}
</style>
