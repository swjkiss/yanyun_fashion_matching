<template>
  <view class="list-page">
    <view v-if="loading && !list.length" class="list-page__grid">
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
      title="你还没有留下衣影"
      hint="挑几件合意的时装，配成一整套"
      action-text="去搭配"
      @action="goCreate"
    />

    <view v-else class="list-page__grid">
      <InkOutfitCard v-for="o in list" :key="o.id" :outfit="o" />
    </view>

    <view v-if="list.length" class="list-page__more">
      <text class="list-page__more-text">{{ finished ? '— 已到底 —' : '加载中…' }}</text>
    </view>

    <view class="list-page__fab u-press" @tap="goCreate">
      <text class="list-page__fab-text">＋</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { fetchMyOutfits } from '@/api'
import { useUserStore } from '@/stores/user'

const PAGE_SIZE = 10
const list = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const finished = ref(false)
const error = ref('')
const user = useUserStore()

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
    await user.ensureLogin()
    const result = await fetchMyOutfits({ pageNo: page.value, pageSize: PAGE_SIZE })
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

function goCreate() {
  uni.navigateTo({ url: '/pages/outfit/create' })
}

onLoad(() => load(true))
onShow(() => {
  if (list.value.length) load(true)
})
onPullDownRefresh(() => load(true))
onReachBottom(() => load(false))
</script>

<style lang="scss" scoped>
.list-page {
  min-height: 100vh;
  background: $paper-100;
  padding: $space-24 $space-32 160rpx;

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

  &__fab {
    position: fixed;
    right: $space-40;
    bottom: calc(#{$space-64} + env(safe-area-inset-bottom));
    width: 104rpx;
    height: 104rpx;
    border-radius: 50%;
    background: $accent;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 12rpx 32rpx -12rpx rgba(166, 50, 42, 0.6);
  }

  &__fab-text {
    font-size: 44rpx;
    color: #fdfbf7;
    line-height: 1;
    margin-top: -4rpx;
  }
}
</style>
