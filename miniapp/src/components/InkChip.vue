<template>
  <view
    class="chip"
    :class="{ 'chip--active': active }"
    :style="active && tone ? { color: tone, borderColor: tone, backgroundColor: withAlpha(tone) } : {}"
    @tap="$emit('click')"
  >
    <view v-if="dot" class="chip__dot" :style="{ backgroundColor: tone || '#918780' }"></view>
    <text class="chip__text">{{ text }}</text>
    <text v-if="count !== null && count !== undefined" class="chip__count">{{ count }}</text>
  </view>
</template>

<script setup>
defineProps({
  text: { type: String, default: '' },
  active: { type: Boolean, default: false },
  /** 主色（稀有度/色系 chip 用） */
  tone: { type: String, default: '' },
  dot: { type: Boolean, default: false },
  count: { type: [Number, String], default: null },
})
defineEmits(['click'])

function withAlpha(hex) {
  if (!hex || hex.length !== 7) return 'rgba(166,50,42,0.06)'
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r},${g},${b},0.08)`
}
</script>

<style lang="scss" scoped>
.chip {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  height: 60rpx;
  padding: 0 $space-24;
  border-radius: $radius-4;
  border: 1px solid $ink-100;
  background: $paper-50;
  transition: all $motion-fast $motion-ease;

  &--active {
    border-color: $accent;
    background: rgba(166, 50, 42, 0.06);

    .chip__text {
      color: $accent;
    }
  }

  &__dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
  }

  &__text {
    font-size: $text-24;
    color: $ink-600;
    letter-spacing: 1rpx;
  }

  &__count {
    font-size: 20rpx;
    color: $ink-300;
  }
}
</style>
