<template>
  <view class="search">
    <view class="search__status" :style="{ height: statusHeight + 'px' }"></view>

    <!-- 顶部搜索条 -->
    <view class="search__bar">
      <view class="search__back u-press" @tap="back">
        <text class="search__back-icon">‹</text>
      </view>
      <view class="search__input-wrap">
        <text class="search__input-icon">⌕</text>
        <input
          v-model="keyword"
          class="search__input"
          type="text"
          confirm-type="search"
          placeholder="搜索时装 · 系列 · 标签"
          placeholder-class="search__ph"
          @confirm="onSearch"
          @input="onInput"
        />
        <text v-if="keyword" class="search__clear" @tap="clearKeyword">×</text>
      </view>
      <view class="search__filter u-press" @tap="openFilter">
        <text class="search__filter-text">筛选</text>
        <text v-if="activeCount" class="search__filter-dot">{{ activeCount }}</text>
      </view>
    </view>

    <!-- 已选条件 -->
    <scroll-view v-if="chips.length" class="search__chips" scroll-x :show-scrollbar="false">
      <view class="search__chips-inner">
        <InkChip
          v-for="chip in chips"
          :key="chip.key"
          :text="chip.text"
          active
          @click="removeChip(chip)"
        />
        <view class="search__clear-all u-press" @tap="resetAll">
          <text class="search__clear-all-text">清空</text>
        </view>
      </view>
    </scroll-view>

    <!-- 搜索历史 -->
    <view v-if="!keyword && !activeCount && history.length" class="search__history">
      <view class="search__history-head">
        <text class="search__history-title">最近搜索</text>
        <text class="search__history-clear" @tap="clearHistory">清空</text>
      </view>
      <view class="search__history-list">
        <InkChip v-for="h in history" :key="h" :text="h" @click="useHistory(h)" />
      </view>
    </view>

    <!-- 结果 -->
    <view class="search__body">
      <view v-if="loading && !list.length" class="search__grid">
        <InkSkeleton v-for="n in 4" :key="n" cover-height="380rpx" />
      </view>

      <InkEmpty
        v-else-if="error"
        title="加载失败"
        :hint="error"
        action-text="重试"
        @action="load(true)"
      />

      <InkEmpty
        v-else-if="!list.length && !loading"
        title="未寻得此衣"
        hint="换个关键词，或放宽筛选条件"
        action-text="重置条件"
        @action="resetAll"
      />

      <view v-else class="search__grid">
        <InkItemCard v-for="item in list" :key="item.id" :item="item" />
      </view>

      <view v-if="list.length" class="search__more">
        <text class="search__more-text">{{ finished ? '— 已到底 —' : '加载中…' }}</text>
      </view>
    </view>

    <!-- 筛选抽屉 -->
    <view v-if="filterOpen" class="sheet" @tap="closeFilter">
      <view class="sheet__panel" @tap.stop>
        <view class="sheet__head">
          <text class="sheet__title">筛选</text>
          <text class="sheet__close" @tap="closeFilter">×</text>
        </view>
        <scroll-view class="sheet__body" scroll-y>
          <view v-for="group in groups" :key="group.key" class="sheet__group">
            <text class="sheet__group-title">{{ group.title }}</text>
            <view class="sheet__group-items">
              <InkChip
                v-for="opt in group.options"
                :key="opt.value"
                :text="opt.text"
                :tone="opt.color"
                :dot="!!opt.color"
                :count="group.key !== 'sort' ? opt.count : null"
                :active="isActive(group.key, opt.value)"
                @click="pick(group.key, opt.value)"
              />
            </view>
          </view>
        </scroll-view>
        <view class="sheet__foot">
          <view class="sheet__reset u-press" @tap="resetAll">
            <text class="sheet__reset-text">重置</text>
          </view>
          <view class="sheet__confirm u-press" @tap="applyFilter">
            <text class="sheet__confirm-text">确定</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { fetchFilters, fetchItems } from '@/api'
import { HISTORY_KEY, statusBarHeight } from '@/config'

const PAGE_SIZE = 10

const statusHeight = ref(statusBarHeight())
const keyword = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const finished = ref(false)
const error = ref('')
const history = ref([])

const filters = ref({})
const form = ref({ category: '', rarity: '', colorFamily: '', style: '', sort: 'latest' })
const draft = ref({})
const filterOpen = ref(false)

const GROUPS = [
  { key: 'category', title: '部位', dict: 'categories' },
  { key: 'rarity', title: '稀有度', dict: 'rarities' },
  { key: 'colorFamily', title: '色系', dict: 'colors' },
  { key: 'style', title: '风格', dict: 'styles' },
  { key: 'sort', title: '排序', dict: 'sorts' },
]

const groups = computed(() =>
  GROUPS.map((g) => ({ key: g.key, title: g.title, options: filters.value[g.dict] || [] }))
)

const activeCount = computed(
  () => Object.entries(form.value).filter(([k, v]) => v && !(k === 'sort' && v === 'latest')).length
)

const chips = computed(() => {
  const out = []
  GROUPS.forEach((g) => {
    const value = form.value[g.key]
    if (!value) return
    if (g.key === 'sort' && value === 'latest') return
    const options = filters.value[g.dict] || []
    const hit = options.find((o) => o.value === value)
    out.push({ key: g.key, value, text: hit ? hit.text : value })
  })
  return out
})

function readHistory() {
  try {
    history.value = JSON.parse(uni.getStorageSync(HISTORY_KEY) || '[]')
  } catch (e) {
    history.value = []
  }
}
function pushHistory(word) {
  const w = word.trim()
  if (!w) return
  const next = [w, ...history.value.filter((h) => h !== w)].slice(0, 8)
  history.value = next
  uni.setStorageSync(HISTORY_KEY, JSON.stringify(next))
}
function clearHistory() {
  history.value = []
  uni.removeStorageSync(HISTORY_KEY)
}

async function loadFilters() {
  try {
    filters.value = await fetchFilters()
  } catch (e) {
    /* 筛选面板失败不阻塞列表 */
  }
}

async function load(reset = false) {
  if (loading.value) return
  if (!reset && finished.value) return
  loading.value = true
  error.value = ''
  if (reset) {
    page.value = 1
    finished.value = false
  }
  try {
    const params = {
      pageNo: page.value,
      pageSize: PAGE_SIZE,
      sort: form.value.sort || 'latest',
    }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    ;['category', 'rarity', 'colorFamily', 'style'].forEach((k) => {
      if (form.value[k]) params[k] = form.value[k]
    })
    const result = await fetchItems(params)
    const records = result.records || []
    list.value = reset ? records : list.value.concat(records)
    total.value = result.total || 0
    finished.value = list.value.length >= total.value || records.length < PAGE_SIZE
    if (!finished.value) page.value += 1
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function onSearch() {
  pushHistory(keyword.value)
  load(true)
}
function onInput() {
  // 输入清空后自动回到全量
  if (!keyword.value) load(true)
}
function useHistory(word) {
  keyword.value = word
  load(true)
}
function clearKeyword() {
  keyword.value = ''
  load(true)
}

function openFilter() {
  draft.value = { ...form.value }
  filterOpen.value = true
}
function closeFilter() {
  filterOpen.value = false
}
function isActive(key, value) {
  return (draft.value[key] || '') === value
}
function pick(key, value) {
  draft.value = { ...draft.value, [key]: draft.value[key] === value ? '' : value }
}
function applyFilter() {
  form.value = { ...draft.value, sort: draft.value.sort || 'latest' }
  filterOpen.value = false
  load(true)
}
function removeChip(chip) {
  form.value = { ...form.value, [chip.key]: '' }
  load(true)
}
function resetAll() {
  form.value = { category: '', rarity: '', colorFamily: '', style: '', sort: 'latest' }
  filterOpen.value = false
  load(true)
}
function back() {
  uni.navigateBack()
}

onLoad((options) => {
  readHistory()
  loadFilters()
  if (options && options.category) {
    form.value.category = options.category
    draft.value = { ...form.value }
  }
  load(true)
})
onPullDownRefresh(() => load(true))
onReachBottom(() => load(false))
</script>

<style lang="scss" scoped>
.search {
  min-height: 100vh;
  background: $paper-100;

  &__status {
    width: 100%;
  }

  &__bar {
    display: flex;
    align-items: center;
    gap: $space-16;
    padding: $space-12 $space-32 $space-20;
  }

  &__back {
    width: 48rpx;
    height: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-left: -10rpx;
  }

  &__back-icon {
    font-size: 52rpx;
    color: $ink-700;
    line-height: 1;
    margin-top: -6rpx;
  }

  &__input-wrap {
    flex: 1;
    height: 72rpx;
    border-radius: $radius-8;
    background: $paper-50;
    border: 1px solid $ink-100;
    display: flex;
    align-items: center;
    padding: 0 $space-20;
    gap: 10rpx;
  }

  &__input-icon {
    font-size: 28rpx;
    color: $ink-300;
  }

  &__input {
    flex: 1;
    font-size: $text-26;
    color: $ink-800;
    height: 100%;
  }

  &__ph {
    color: $ink-300;
    font-size: $text-26;
  }

  &__clear {
    font-size: 32rpx;
    color: $ink-300;
    padding: 0 6rpx;
  }

  &__filter {
    position: relative;
    height: 72rpx;
    padding: 0 $space-24;
    border-radius: $radius-8;
    border: 1px solid $ink-100;
    background: $paper-50;
    display: flex;
    align-items: center;
  }

  &__filter-text {
    font-size: $text-26;
    color: $ink-700;
    letter-spacing: 2rpx;
  }

  &__filter-dot {
    position: absolute;
    top: -8rpx;
    right: -8rpx;
    min-width: 30rpx;
    height: 30rpx;
    padding: 0 6rpx;
    border-radius: 999rpx;
    background: $accent;
    color: #fdfbf7;
    font-size: 18rpx;
    line-height: 30rpx;
    text-align: center;
  }

  &__chips {
    white-space: nowrap;
  }

  &__chips-inner {
    display: flex;
    align-items: center;
    gap: 12rpx;
    padding: 0 $space-32 $space-20;
  }

  &__clear-all {
    padding: 0 $space-20;
    height: 60rpx;
    display: flex;
    align-items: center;
  }

  &__clear-all-text {
    font-size: 22rpx;
    color: $ink-300;
  }

  &__history {
    padding: $space-8 $space-32 $space-24;
  }

  &__history-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $space-16;
  }

  &__history-title {
    font-size: 22rpx;
    color: $ink-400;
    letter-spacing: 2rpx;
  }

  &__history-clear {
    font-size: 20rpx;
    color: $ink-300;
  }

  &__history-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
  }

  &__body {
    padding: $space-8 $space-32 96rpx;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: $space-24;
  }

  &__more {
    padding: $space-40 0;
    text-align: center;
  }

  &__more-text {
    font-size: 20rpx;
    color: $ink-300;
    letter-spacing: 2rpx;
  }
}

/* ---------- 底部抽屉 ---------- */
.sheet {
  position: fixed;
  inset: 0;
  background: rgba(16, 14, 12, 0.42);
  z-index: 100;
  display: flex;
  align-items: flex-end;

  &__panel {
    width: 100%;
    max-height: 78vh;
    background: $paper-50;
    border-radius: $radius-16 $radius-16 0 0;
    display: flex;
    flex-direction: column;
    padding-bottom: constant(safe-area-inset-bottom);
    padding-bottom: env(safe-area-inset-bottom);
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-28 $space-32 $space-16;
    border-bottom: 1px solid $ink-100;
  }

  &__title {
    font-family: $font-display;
    font-size: $text-32;
    letter-spacing: 4rpx;
    color: $ink-900;
  }

  &__close {
    font-size: 40rpx;
    color: $ink-300;
    line-height: 1;
  }

  &__body {
    flex: 1;
    max-height: 56vh;
    padding: 0 $space-32;
  }

  &__group {
    padding: $space-24 0;
    border-bottom: 1px solid rgba(217, 208, 198, 0.5);
  }

  &__group-title {
    display: block;
    font-size: 22rpx;
    color: $ink-400;
    letter-spacing: 2rpx;
    margin-bottom: $space-16;
  }

  &__group-items {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
  }

  &__foot {
    display: flex;
    gap: $space-20;
    padding: $space-20 $space-32 $space-28;
    border-top: 1px solid $ink-100;
  }

  &__reset {
    flex: 0 0 200rpx;
    height: 84rpx;
    border-radius: $radius-8;
    border: 1px solid $ink-200;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__reset-text {
    font-size: $text-28;
    color: $ink-600;
    letter-spacing: 4rpx;
  }

  &__confirm {
    flex: 1;
    height: 84rpx;
    border-radius: $radius-8;
    background: $accent;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__confirm-text {
    font-size: $text-28;
    color: #fdfbf7;
    letter-spacing: 6rpx;
  }
}
</style>
