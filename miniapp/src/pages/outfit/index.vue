<template>
  <view class="plaza">
    <view class="plaza__status" :style="{ height: statusHeight + 'px' }"></view>

    <view class="plaza__head">
      <view class="plaza__head-left">
        <text class="plaza__title">搭配广场</text>
        <text class="plaza__sub">江湖同袍，各成一体</text>
      </view>
      <view class="plaza__create u-press" @tap="goCreate">
        <text class="plaza__create-icon">＋</text>
        <text class="plaza__create-text">搭配</text>
      </view>
    </view>

    <!-- 排序切换 -->
    <view class="plaza__tabs">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="plaza__tab"
        :class="{ 'plaza__tab--active': sort === tab.value }"
        @tap="switchSort(tab.value)"
      >
        <text class="plaza__tab-text">{{ tab.text }}</text>
        <view v-if="sort === tab.value" class="plaza__tab-line"></view>
      </view>
      <view class="plaza__tabs-hold"></view>
    </view>

    <view class="plaza__body">
      <view v-if="loading && !list.length" class="plaza__grid">
        <InkSkeleton v-for="n in 4" :key="n" cover-height="440rpx" />
      </view>

      <InkEmpty
        v-else-if="error"
        title="加载失败"
        :hint="error"
        action-text="重试"
        @action="load(true)"
      />

      <InkEmpty
        v-else-if="!list.length"
        title="尚无搭配"
        hint="做第一个留下衣影的人"
        action-text="去搭配"
        @action="goCreate"
      />

      <view v-else class="plaza__grid">
        <InkOutfitCard v-for="o in list" :key="o.id" :outfit="o" />
      </view>

      <view v-if="list.length" class="plaza__more">
        <text class="plaza__more-text">{{ finished ? '— 已到底 —' : '加载中…' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { fetchOutfits } from '@/api'
import { statusBarHeight } from '@/config'

const PAGE_SIZE = 10
const tabs = [
  { value: 'latest', text: '最新' },
  { value: 'hot', text: '最热' },
]

const statusHeight = ref(statusBarHeight())
const sort = ref('latest')
const list = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const finished = ref(false)
const error = ref('')

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
    const result = await fetchOutfits({ pageNo: page.value, pageSize: PAGE_SIZE, sort: sort.value })
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

function switchSort(value) {
  if (sort.value === value) return
  sort.value = value
  load(true)
}
function goCreate() {
  uni.navigateTo({ url: '/pages/outfit/create' })
}

onLoad(() => load(true))
onShow(() => {
  // 从创建页返回时刷新，保证新搭配立即出现
  if (list.value.length) load(true)
})
onPullDownRefresh(() => load(true))
onReachBottom(() => load(false))
</script>

<style lang="scss" scoped>
.plaza {
  min-height: 100vh;
  background: $paper-100;
  padding-bottom: 48rpx;

  &__status {
    width: 100%;
  }

  &__head {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    padding: $space-16 $space-32 $space-8;
  }

  &__title {
    display: block;
    font-family: $font-display;
    font-size: 44rpx;
    letter-spacing: 6rpx;
    color: $ink-900;
  }

  &__sub {
    display: block;
    margin-top: 6rpx;
    font-size: 20rpx;
    letter-spacing: 3rpx;
    color: $ink-300;
  }

  &__create {
    display: flex;
    align-items: center;
    gap: 6rpx;
    height: 64rpx;
    padding: 0 $space-24;
    border-radius: $radius-8;
    background: $accent;
  }

  &__create-icon {
    font-size: 26rpx;
    color: #fdfbf7;
    line-height: 1;
  }

  &__create-text {
    font-size: 22rpx;
    color: #fdfbf7;
    letter-spacing: 2rpx;
  }

  &__tabs {
    display: flex;
    align-items: center;
    gap: $space-40;
    padding: $space-20 $space-32 $space-16;
  }

  &__tab {
    position: relative;
    padding-bottom: 10rpx;
  }

  &__tab-text {
    font-size: $text-28;
    color: $ink-400;
    letter-spacing: 3rpx;
  }

  &__tab--active .plaza__tab-text {
    color: $ink-900;
    font-weight: $weight-medium;
  }

  &__tab-line {
    position: absolute;
    left: 50%;
    bottom: 0;
    transform: translateX(-50%);
    width: 28rpx;
    height: 4rpx;
    border-radius: 999rpx;
    background: $accent;
  }

  &__tabs-hold {
    flex: 1;
  }

  &__body {
    padding: $space-8 $space-32 48rpx;
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
</style>
