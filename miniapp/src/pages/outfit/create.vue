<template>
  <view class="create">
    <scroll-view class="create__scroll" scroll-y>
      <!-- 标题 -->
      <view class="create__field">
        <text class="create__label">题名</text>
        <input
          v-model="form.title"
          class="create__input create__input--title"
          placeholder="给这套行头取个名字"
          placeholder-class="create__ph"
          maxlength="40"
        />
      </view>

      <view class="create__field">
        <text class="create__label">一句</text>
        <input
          v-model="form.subtitle"
          class="create__input"
          placeholder="一句话点题，如「一身黑衣，半生风雪」"
          placeholder-class="create__ph"
          maxlength="60"
        />
      </view>

      <view class="create__field">
        <text class="create__label">叙述</text>
        <textarea
          v-model="form.description"
          class="create__textarea"
          placeholder="说说这套搭配的来历、场合或心思"
          placeholder-class="create__ph"
          maxlength="500"
          auto-height
        />
      </view>

      <!-- 风格 -->
      <view class="create__field">
        <text class="create__label">风格</text>
        <view class="create__chips">
          <InkChip
            v-for="s in styles"
            :key="s.value"
            :text="s.text"
            :active="form.styleTag === s.value"
            @click="form.styleTag = form.styleTag === s.value ? '' : s.value"
          />
        </view>
      </view>

      <!-- 场景 -->
      <view class="create__field">
        <text class="create__label">场景</text>
        <view class="create__chips">
          <InkChip
            v-for="sc in scenes"
            :key="sc"
            :text="sc"
            :active="form.sceneTag === sc"
            @click="form.sceneTag = form.sceneTag === sc ? '' : sc"
          />
        </view>
      </view>

      <!-- 选衣 -->
      <view class="create__field">
        <view class="create__field-head">
          <text class="create__label">行头</text>
          <text class="create__hint">{{ selected.length }}/6</text>
        </view>
        <view v-if="selected.length" class="create__selected">
          <view
            v-for="(it, i) in selected"
            :key="it.id"
            class="create__thumb"
            @tap="removeItem(i)"
          >
            <image class="create__thumb-img" :src="img(it.imageUrl)" mode="aspectFill" />
            <view class="create__thumb-x">
              <text class="create__thumb-x-text">×</text>
            </view>
          </view>
          <view v-if="selected.length < 6" class="create__thumb create__thumb--add" @tap="openPicker">
            <text class="create__thumb-add">＋</text>
          </view>
        </view>
        <view v-else class="create__empty u-press" @tap="openPicker">
          <text class="create__empty-text">＋ 从图鉴中挑选时装</text>
        </view>
      </view>

      <view class="create__bottom-hold"></view>
    </scroll-view>

    <!-- 提交 -->
    <view class="create__bar">
      <view class="create__bar-info">
        <text class="create__bar-title">{{ form.title || '未命名搭配' }}</text>
        <text class="create__bar-sub">{{ selected.length }} 件 · {{ styleText || '未选风格' }}</text>
      </view>
      <view
        class="create__submit u-press"
        :class="{ 'create__submit--disabled': !canSubmit || submitting }"
        @tap="submit"
      >
        <text class="create__submit-text">{{ submitting ? '发布中' : '发表' }}</text>
      </view>
    </view>

    <!-- 选衣弹层 -->
    <view v-if="pickerOpen" class="picker">
      <view class="picker__mask" @tap="pickerOpen = false"></view>
      <view class="picker__panel">
        <view class="picker__head">
          <text class="picker__title">挑选时装</text>
          <text class="picker__done" @tap="pickerOpen = false">完成</text>
        </view>
        <view class="picker__search">
          <text class="picker__search-icon">⌕</text>
          <input
            v-model="pickerKeyword"
            class="picker__search-input"
            placeholder="搜索时装名"
            placeholder-class="create__ph"
            @input="debouncedSearch"
          />
        </view>
        <scroll-view class="picker__list" scroll-y @scrolltolower="loadMore">
          <view
            v-for="it in pickerList"
            :key="it.id"
            class="picker__row u-press"
            @tap="togglePick(it)"
          >
            <image class="picker__img" :src="img(it.imageUrl)" mode="aspectFill" lazy-load />
            <view class="picker__info">
              <text class="picker__name u-ellipsis">{{ it.name }}</text>
              <view class="picker__meta">
                <InkRarity :value="it.rarity" :text="it.rarityText" :color="it.rarityColor" />
                <text class="picker__cat">{{ it.categoryText }}</text>
              </view>
            </view>
            <view class="picker__check" :class="{ 'picker__check--on': isPicked(it.id) }">
              <text class="picker__check-text">{{ isPicked(it.id) ? '✓' : '' }}</text>
            </view>
          </view>
          <view class="picker__more">
            <text class="picker__more-text">{{ pickerFinished ? '— 已到底 —' : '加载中…' }}</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createOutfit, fetchFilters, fetchItems, fetchItemDetail } from '@/api'
import { resolveImage } from '@/config'

const SCENES = ['夜行', '朝会', '雅集', '踏雪', '征战', '远行', '山居', '市井']
const MAX_ITEMS = 6

const img = (p) => resolveImage(p)

const form = ref({ title: '', subtitle: '', description: '', styleTag: '', sceneTag: '' })
const styles = ref([])
const scenes = ref(SCENES)
const selected = ref([])
const submitting = ref(false)

const pickerOpen = ref(false)
const pickerKeyword = ref('')
const pickerList = ref([])
const pickerPage = ref(1)
const pickerFinished = ref(false)
const pickerLoading = ref(false)
let searchTimer = null

const canSubmit = computed(() => form.value.title.trim() && selected.value.length > 0)

const styleText = computed(() => {
  const hit = styles.value.find((s) => s.value === form.value.styleTag)
  return hit ? hit.text : ''
})

async function bootstrap() {
  try {
    const f = await fetchFilters()
    styles.value = f.styles || []
  } catch (e) {
    /* 忽略 */
  }
}

async function loadPicker(reset = false) {
  if (pickerLoading.value) return
  if (!reset && pickerFinished.value) return
  pickerLoading.value = true
  if (reset) {
    pickerPage.value = 1
    pickerFinished.value = false
  }
  try {
    const result = await fetchItems({
      pageNo: pickerPage.value,
      pageSize: 20,
      keyword: pickerKeyword.value.trim() || undefined,
    })
    const records = result.records || []
    pickerList.value = reset ? records : pickerList.value.concat(records)
    pickerFinished.value = records.length < 20 || pickerList.value.length >= (result.total || 0)
    if (!pickerFinished.value) pickerPage.value += 1
  } catch (e) {
    /* ignore */
  } finally {
    pickerLoading.value = false
  }
}

function debouncedSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadPicker(true), 320)
}

function openPicker() {
  pickerOpen.value = true
  if (!pickerList.value.length) loadPicker(true)
}
function loadMore() {
  loadPicker(false)
}

function isPicked(id) {
  return selected.value.some((s) => s.id === id)
}

function togglePick(it) {
  if (isPicked(it.id)) {
    selected.value = selected.value.filter((s) => s.id !== it.id)
    return
  }
  if (selected.value.length >= MAX_ITEMS) {
    uni.showToast({ title: `最多 ${MAX_ITEMS} 件`, icon: 'none' })
    return
  }
  selected.value = selected.value.concat(it)
}

function removeItem(index) {
  selected.value = selected.value.filter((_, i) => i !== index)
}

async function submit() {
  if (submitting.value) return
  if (!form.value.title.trim()) {
    uni.showToast({ title: '请填写题名', icon: 'none' })
    return
  }
  if (!selected.value.length) {
    uni.showToast({ title: '请至少挑一件时装', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await createOutfit({
      ...form.value,
      title: form.value.title.trim(),
      itemIds: selected.value.map((s) => s.id),
      colorTone: selected.value[0]?.colorFamily || '',
      coverImage: selected.value[0]?.imageUrl || '',
    })
    uni.showToast({ title: '已发表', icon: 'none' })
    setTimeout(() => {
      uni.navigateBack({ delta: 1 })
    }, 620)
  } catch (e) {
    /* request 层已提示 */
  } finally {
    submitting.value = false
  }
}

onLoad(async (options) => {
  await bootstrap()
  if (options && options.itemId) {
    try {
      const data = await fetchItemDetail(options.itemId)
      if (data.item) selected.value = [data.item]
      form.value.styleTag = data.item.style || ''
    } catch (e) {
      /* ignore */
    }
  }
})
</script>

<style lang="scss" scoped>
.create {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: $paper-100;

  &__scroll {
    flex: 1;
    height: 0;
  }

  &__field {
    padding: $space-28 $space-32 0;
  }

  &__field-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__label {
    display: block;
    font-size: 22rpx;
    color: $ink-400;
    letter-spacing: 4rpx;
    margin-bottom: $space-16;
  }

  &__hint {
    font-size: 20rpx;
    color: $ink-300;
  }

  &__input {
    width: 100%;
    height: 80rpx;
    background: $paper-50;
    border: 1px solid $ink-100;
    border-radius: $radius-8;
    padding: 0 $space-20;
    font-size: $text-28;
    color: $ink-900;
  }

  &__input--title {
    font-family: $font-display;
    font-size: $text-32;
    letter-spacing: 2rpx;
  }

  &__textarea {
    width: 100%;
    min-height: 160rpx;
    background: $paper-50;
    border: 1px solid $ink-100;
    border-radius: $radius-8;
    padding: $space-20;
    font-size: $text-26;
    color: $ink-800;
    line-height: 1.8;
  }

  &__ph {
    color: $ink-300;
    font-size: 24rpx;
  }

  &__chips {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
  }

  &__selected {
    display: flex;
    flex-wrap: wrap;
    gap: $space-16;
  }

  &__thumb {
    position: relative;
    width: 132rpx;
    height: 176rpx;
    border-radius: $radius-8;
    overflow: hidden;
    background: $paper-200;
    border: 1px solid $ink-100;
  }

  &__thumb-img {
    width: 100%;
    height: 100%;
    display: block;
  }

  &__thumb-x {
    position: absolute;
    top: 0;
    right: 0;
    width: 36rpx;
    height: 36rpx;
    background: rgba(16, 14, 12, 0.62);
    border-bottom-left-radius: $radius-8;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__thumb-x-text {
    color: #fdfbf7;
    font-size: 24rpx;
    line-height: 1;
  }

  &__thumb--add {
    display: flex;
    align-items: center;
    justify-content: center;
    border-style: dashed;
    border-color: $ink-200;
    background: transparent;
  }

  &__thumb-add {
    font-size: 44rpx;
    color: $ink-300;
  }

  &__empty {
    height: 168rpx;
    border: 1px dashed $ink-200;
    border-radius: $radius-8;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__empty-text {
    font-size: 24rpx;
    color: $ink-400;
    letter-spacing: 2rpx;
  }

  &__bottom-hold {
    height: 220rpx;
  }

  &__bar {
    display: flex;
    align-items: center;
    gap: $space-20;
    padding: $space-20 $space-32;
    padding-bottom: calc(#{$space-20} + env(safe-area-inset-bottom));
    background: $paper-50;
    border-top: 1px solid $ink-100;
  }

  &__bar-info {
    flex: 1;
    min-width: 0;
  }

  &__bar-title {
    display: block;
    font-family: $font-display;
    font-size: $text-28;
    color: $ink-900;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  &__bar-sub {
    display: block;
    margin-top: 4rpx;
    font-size: 20rpx;
    color: $ink-300;
  }

  &__submit {
    width: 200rpx;
    height: 84rpx;
    border-radius: $radius-8;
    background: $accent;
    display: flex;
    align-items: center;
    justify-content: center;

    &--disabled {
      background: $ink-200;
    }
  }

  &__submit-text {
    font-size: $text-28;
    color: #fdfbf7;
    letter-spacing: 6rpx;
  }
}

/* ---------- 选衣弹层 ---------- */
.picker {
  position: fixed;
  inset: 0;
  z-index: 120;

  &__mask {
    position: absolute;
    inset: 0;
    background: rgba(16, 14, 12, 0.42);
  }

  &__panel {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 82vh;
    background: $paper-50;
    border-radius: $radius-16 $radius-16 0 0;
    display: flex;
    flex-direction: column;
    padding-bottom: env(safe-area-inset-bottom);
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-28 $space-32 $space-16;
  }

  &__title {
    font-family: $font-display;
    font-size: $text-32;
    letter-spacing: 4rpx;
    color: $ink-900;
  }

  &__done {
    font-size: 26rpx;
    color: $accent;
    letter-spacing: 2rpx;
  }

  &__search {
    margin: 0 $space-32 $space-16;
    height: 72rpx;
    border-radius: $radius-8;
    background: $paper-100;
    display: flex;
    align-items: center;
    padding: 0 $space-20;
    gap: 10rpx;
  }

  &__search-icon {
    font-size: 28rpx;
    color: $ink-300;
  }

  &__search-input {
    flex: 1;
    font-size: 24rpx;
    color: $ink-800;
    height: 100%;
  }

  &__list {
    flex: 1;
    height: 0;
    padding: 0 $space-32;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: $space-20;
    padding: $space-16 0;
    border-bottom: 1px solid rgba(217, 208, 198, 0.5);
  }

  &__img {
    width: 96rpx;
    height: 128rpx;
    border-radius: $radius-8;
    background: $paper-200;
    flex: 0 0 96rpx;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    display: block;
    font-family: $font-display;
    font-size: $text-28;
    color: $ink-900;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 10rpx;
    margin-top: 8rpx;
  }

  &__cat {
    font-size: 20rpx;
    color: $ink-400;
  }

  &__check {
    width: 44rpx;
    height: 44rpx;
    border-radius: 50%;
    border: 1px solid $ink-200;
    display: flex;
    align-items: center;
    justify-content: center;

    &--on {
      background: $accent;
      border-color: $accent;
    }
  }

  &__check-text {
    font-size: 22rpx;
    color: #fdfbf7;
    line-height: 1;
  }

  &__more {
    padding: $space-28 0 $space-48;
    text-align: center;
  }

  &__more-text {
    font-size: 20rpx;
    color: $ink-300;
  }
}
</style>
