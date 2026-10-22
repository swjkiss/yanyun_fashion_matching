<template>
  <view class="item-card u-press" @tap="go">
    <view class="item-card__media" :style="{ backgroundColor: bgColor }">
      <image
        class="item-card__img"
        :src="cover"
        mode="aspectFill"
        lazy-load
        @error="imgFailed = true"
      />
      <view v-if="imgFailed" class="item-card__fallback">
        <text class="item-card__glyph">{{ item.glyph || '衣' }}</text>
      </view>
      <view class="item-card__veil"></view>
      <view class="item-card__badge">
        <InkRarity :value="item.rarity" :text="item.rarityText" :color="item.rarityColor" />
      </view>
      <view class="item-card__cat">{{ item.categoryText }}</view>
    </view>
    <view class="item-card__body">
      <text class="item-card__name u-ellipsis">{{ item.name }}</text>
      <view class="item-card__meta">
        <view class="item-card__tone" :style="{ backgroundColor: item.colorValue || '#B7ADA4' }"></view>
        <text class="item-card__color">{{ item.colorText }}</text>
        <text class="item-card__dot">·</text>
        <text class="item-card__style">{{ item.styleText }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { resolveImage } from '@/config'

const props = defineProps({
  item: { type: Object, required: true },
})

const imgFailed = ref(false)
const cover = computed(() => resolveImage(props.item.imageUrl))
const bgColor = computed(() => props.item.rarityBg || '#EFE7D9')

function go() {
  uni.navigateTo({ url: `/pages/item/detail?id=${props.item.id}` })
}
</script>

<style lang="scss" scoped>
.item-card {
  background: $paper-50;
  border: 1px solid $ink-100;
  border-radius: $radius-12;
  overflow: hidden;
  box-shadow: $shadow-sm;

  &__media {
    position: relative;
    width: 100%;
    height: 440rpx;
    overflow: hidden;
  }

  &__img {
    width: 100%;
    height: 100%;
    display: block;
  }

  &__fallback {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__glyph {
    font-family: $font-display;
    font-size: 160rpx;
    color: rgba(27, 24, 21, 0.18);
  }

  &__veil {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 45%;
    background: linear-gradient(180deg, rgba(16, 14, 12, 0) 0%, rgba(16, 14, 12, 0.42) 100%);
  }

  &__badge {
    position: absolute;
    left: $space-16;
    top: $space-16;
  }

  &__cat {
    position: absolute;
    right: $space-16;
    bottom: $space-16;
    font-size: 20rpx;
    color: rgba(253, 251, 247, 0.92);
    letter-spacing: 2rpx;
  }

  &__body {
    padding: $space-20;
  }

  &__name {
    display: block;
    font-family: $font-display;
    font-size: $text-28;
    color: $ink-900;
    letter-spacing: 1rpx;
  }

  &__meta {
    margin-top: 10rpx;
    display: flex;
    align-items: center;
    gap: 8rpx;
  }

  &__tone {
    width: 14rpx;
    height: 14rpx;
    border-radius: 50%;
    border: 1px solid rgba(27, 24, 21, 0.12);
  }

  &__color,
  &__style {
    font-size: 20rpx;
    color: $ink-400;
  }

  &__dot {
    font-size: 20rpx;
    color: $ink-300;
  }
}
</style>
