<template>
  <view class="outfit-card u-press" @tap="go">
    <view class="outfit-card__media">
      <image class="outfit-card__img" :src="cover" mode="aspectFill" lazy-load />
      <view class="outfit-card__veil"></view>
      <view class="outfit-card__tone" :style="{ backgroundColor: toneColor }"></view>
      <view class="outfit-card__count">{{ outfit.itemCount || 0 }} 件</view>
      <view class="outfit-card__text">
        <text class="outfit-card__title u-ellipsis">{{ outfit.title }}</text>
        <text class="outfit-card__sub u-ellipsis">{{ outfit.subtitle || outfit.sceneTag }}</text>
      </view>
    </view>
    <view class="outfit-card__foot">
      <view class="outfit-card__author">
        <image class="outfit-card__avatar" :src="avatar" mode="aspectFill" />
        <text class="outfit-card__name u-ellipsis">{{ outfit.authorName }}</text>
      </view>
      <view class="outfit-card__fav">
        <text class="outfit-card__fav-icon">{{ outfit.favorited ? '★' : '☆' }}</text>
        <text class="outfit-card__fav-num">{{ outfit.favCount || 0 }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { resolveImage } from '@/config'

const props = defineProps({
  outfit: { type: Object, required: true },
})

const cover = computed(() => resolveImage(props.outfit.coverImage))
const avatar = computed(() => resolveImage(props.outfit.authorAvatar) || '')
const toneColor = computed(() => props.outfit.colorValue || '#B08A45')

function go() {
  uni.navigateTo({ url: `/pages/outfit/detail?id=${props.outfit.id}` })
}
</script>

<style lang="scss" scoped>
.outfit-card {
  background: $paper-50;
  border: 1px solid $ink-100;
  border-radius: $radius-12;
  overflow: hidden;
  box-shadow: $shadow-sm;

  &__media {
    position: relative;
    height: 460rpx;
    overflow: hidden;
  }

  &__img {
    width: 100%;
    height: 100%;
    display: block;
  }

  &__veil {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 60%;
    background: linear-gradient(180deg, rgba(16, 14, 12, 0) 0%, rgba(16, 14, 12, 0.66) 100%);
  }

  &__tone {
    position: absolute;
    left: $space-16;
    top: $space-16;
    width: 8rpx;
    height: 40rpx;
    border-radius: 999rpx;
    opacity: 0.9;
  }

  &__count {
    position: absolute;
    right: $space-16;
    top: $space-16;
    padding: 2rpx 12rpx;
    border-radius: $radius-4;
    background: rgba(16, 14, 12, 0.34);
    color: rgba(253, 251, 247, 0.9);
    font-size: 20rpx;
  }

  &__text {
    position: absolute;
    left: $space-20;
    right: $space-20;
    bottom: $space-20;
  }

  &__title {
    display: block;
    font-family: $font-display;
    font-size: $text-32;
    color: #fdfbf7;
    letter-spacing: $tracking-wide;
    text-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.35);
  }

  &__sub {
    display: block;
    margin-top: 6rpx;
    font-size: 20rpx;
    color: rgba(253, 251, 247, 0.78);
    letter-spacing: 1rpx;
  }

  &__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-16 $space-20;
  }

  &__author {
    display: flex;
    align-items: center;
    gap: 10rpx;
    min-width: 0;
    flex: 1;
  }

  &__avatar {
    width: 40rpx;
    height: 40rpx;
    border-radius: 50%;
    background: $paper-300;
    flex: 0 0 40rpx;
  }

  &__name {
    font-size: 20rpx;
    color: $ink-400;
    max-width: 180rpx;
  }

  &__fav {
    display: flex;
    align-items: center;
    gap: 6rpx;
  }

  &__fav-icon {
    font-size: 22rpx;
    color: $gold;
  }

  &__fav-num {
    font-size: 20rpx;
    color: $ink-400;
  }
}
</style>
