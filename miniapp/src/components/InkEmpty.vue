<template>
  <view class="empty">
    <view class="empty__mark">
      <view class="empty__circle"></view>
      <view class="empty__brush empty__brush--1"></view>
      <view class="empty__brush empty__brush--2"></view>
    </view>
    <text class="empty__title">{{ title }}</text>
    <text v-if="hint" class="empty__hint">{{ hint }}</text>
    <view v-if="actionText" class="empty__action u-press" @tap="$emit('action')">
      <text class="empty__action-text">{{ actionText }}</text>
    </view>
  </view>
</template>

<script setup>
defineProps({
  title: { type: String, default: '空空如也' },
  hint: { type: String, default: '' },
  actionText: { type: String, default: '' },
})
defineEmits(['action'])
</script>

<style lang="scss" scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx $space-48;

  &__mark {
    position: relative;
    width: 180rpx;
    height: 180rpx;
    margin-bottom: $space-32;
  }

  &__circle {
    position: absolute;
    inset: 20rpx;
    border-radius: 50%;
    background: radial-gradient(
      circle at 42% 38%,
      rgba(27, 24, 21, 0.07) 0%,
      rgba(27, 24, 21, 0.02) 62%,
      rgba(27, 24, 21, 0) 72%
    );
    border: 1px solid rgba(176, 138, 69, 0.22);
  }

  &__brush {
    position: absolute;
    background: rgba(27, 24, 21, 0.16);
    border-radius: 999rpx;
  }

  &__brush--1 {
    width: 92rpx;
    height: 4rpx;
    left: 44rpx;
    top: 88rpx;
    transform: rotate(-8deg);
  }

  &__brush--2 {
    width: 56rpx;
    height: 3rpx;
    left: 62rpx;
    top: 104rpx;
    opacity: 0.6;
    transform: rotate(-8deg);
  }

  &__title {
    font-family: $font-display;
    font-size: $text-30;
    color: $ink-600;
    letter-spacing: $tracking-wide;
  }

  &__hint {
    margin-top: $space-12;
    font-size: $text-24;
    color: $ink-400;
    text-align: center;
    line-height: 1.7;
  }

  &__action {
    margin-top: $space-40;
    padding: 0 $space-40;
    height: 72rpx;
    border-radius: $radius-8;
    border: 1px solid $accent;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__action-text {
    font-size: $text-26;
    color: $accent;
    letter-spacing: $tracking-wide;
  }
}
</style>
