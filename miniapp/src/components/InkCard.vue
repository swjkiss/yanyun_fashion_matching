<template>
  <view class="card" :class="{ 'card--flat': flat, 'card--pressable': pressable }" @tap="onTap">
    <slot></slot>
  </view>
</template>

<script setup>
const props = defineProps({
  flat: { type: Boolean, default: false },
  pressable: { type: Boolean, default: false },
})
const emit = defineEmits(['click'])

function onTap(e) {
  if (props.pressable) emit('click', e)
}
</script>

<style lang="scss" scoped>
.card {
  background: $paper-50;
  border: 1px solid $ink-100;
  border-radius: $radius-12;
  box-shadow: $shadow-sm;
  overflow: hidden;

  &--flat {
    box-shadow: none;
  }

  &--pressable {
    transition: transform $motion-fast $motion-ease, box-shadow $motion-fast $motion-ease;
  }

  &--pressable:active {
    transform: scale(0.985);
    box-shadow: none;
  }
}
</style>
