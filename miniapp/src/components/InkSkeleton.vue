<template>
  <view class="sk" :class="{ 'sk--row': layout === 'row' }">
    <view class="sk__cover" :style="{ height: coverHeight }">
      <view class="sk__shine"></view>
    </view>
    <view class="sk__body">
      <view class="sk__line sk__line--title"></view>
      <view class="sk__line sk__line--text"></view>
      <view class="sk__line sk__line--short"></view>
    </view>
  </view>
</template>

<script setup>
defineProps({
  /** grid：竖向卡片；row：横向条目 */
  layout: { type: String, default: 'grid' },
  coverHeight: { type: String, default: '400rpx' },
})
</script>

<style lang="scss" scoped>
.sk {
  background: $paper-50;
  border: 1px solid $ink-100;
  border-radius: $radius-12;
  overflow: hidden;

  &--row {
    display: flex;
    gap: $space-20;
    padding: $space-20;

    .sk__cover {
      width: 160rpx;
      flex: 0 0 160rpx;
      border-radius: $radius-8;
    }

    .sk__body {
      flex: 1;
      padding: 0;
    }
  }

  &__cover {
    position: relative;
    background: $paper-200;
    overflow: hidden;
  }

  &__shine {
    position: absolute;
    top: 0;
    left: -60%;
    width: 60%;
    height: 100%;
    background: linear-gradient(
      90deg,
      rgba(255, 255, 255, 0) 0%,
      rgba(255, 255, 255, 0.55) 50%,
      rgba(255, 255, 255, 0) 100%
    );
    animation: sk-shine 1.4s $motion-ease infinite;
  }

  &__body {
    padding: $space-20;
  }

  &__line {
    height: 22rpx;
    border-radius: 999rpx;
    background: $paper-200;
    margin-bottom: $space-16;
  }

  &__line--title {
    width: 62%;
    height: 30rpx;
  }

  &__line--text {
    width: 92%;
  }

  &__line--short {
    width: 40%;
    margin-bottom: 0;
  }
}

@keyframes sk-shine {
  0% {
    left: -60%;
  }
  100% {
    left: 110%;
  }
}
</style>
