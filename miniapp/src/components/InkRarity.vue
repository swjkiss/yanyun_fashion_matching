<template>
  <view
    class="rarity"
    :style="{ color: tone.color, backgroundColor: tone.bg, borderColor: tone.border }"
  >
    <text class="rarity__text">{{ label }}</text>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** 稀有度编码：common/fine/rare/epic/legend */
  value: { type: String, default: 'common' },
  /** 中文文本，缺省按编码兜底 */
  text: { type: String, default: '' },
  /** 服务端下发的稀有度主色，优先使用 */
  color: { type: String, default: '' },
  size: { type: String, default: 'normal' },
})

const PALETTE = {
  common: { color: '#7E8A94', bg: '#F0F3F4', border: '#DDE3E6' },
  fine: { color: '#47655C', bg: '#E4EDEA', border: '#CFDFD9' },
  rare: { color: '#38485A', bg: '#E3E8EE', border: '#CDD6E0' },
  epic: { color: '#8A6A32', bg: '#F6EEDF', border: '#E7D8BC' },
  legend: { color: '#A6322A', bg: '#F7E5E2', border: '#EBCBC6' },
}

const LABELS = {
  common: '凡品',
  fine: '良品',
  rare: '珍品',
  epic: '绝品',
  legend: '传说',
}

const tone = computed(() => {
  const base = PALETTE[props.value] || PALETTE.common
  if (!props.color) return base
  return { ...base, color: props.color }
})

const label = computed(() => props.text || LABELS[props.value] || '凡品')
</script>

<style lang="scss" scoped>
.rarity {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2rpx 14rpx;
  border-radius: $radius-4;
  border-width: 1px;
  border-style: solid;

  &__text {
    font-size: 20rpx;
    letter-spacing: 1rpx;
    line-height: 1.6;
  }
}
</style>
