<template>
  <view class="detail">
    <view v-if="loading" class="detail__loading">
      <InkSkeleton cover-height="900rpx" />
    </view>

    <InkEmpty v-else-if="error" title="加载失败" :hint="error" action-text="重试" @action="load" />

    <template v-else-if="item">
      <!-- 主图 -->
      <view class="detail__hero">
        <image class="detail__img" :src="cover" mode="aspectFill" />
        <view class="detail__hero-veil"></view>
        <view class="detail__hero-text">
          <InkRarity :value="item.rarity" :text="item.rarityText" :color="item.rarityColor" />
          <text class="detail__name">{{ item.name }}</text>
          <view class="detail__hero-meta">
            <view class="detail__tone" :style="{ backgroundColor: item.colorValue }"></view>
            <text class="detail__hero-sub">{{ item.colorText }} · {{ item.styleText }} · {{ item.categoryText }}</text>
          </view>
        </view>
        <view class="detail__glyph-wrap">
          <text class="detail__glyph">{{ item.glyph }}</text>
        </view>
      </view>

      <view class="detail__body">
        <!-- 一句话卖点 -->
        <view v-if="item.summary" class="detail__quote">
          <text class="detail__quote-mark">「</text>
          <text class="detail__quote-text">{{ item.summary }}</text>
        </view>

        <!-- 标签 -->
        <view v-if="item.tagsList && item.tagsList.length" class="detail__tags">
          <InkChip v-for="t in item.tagsList" :key="t" :text="t" />
        </view>

        <!-- 属性表 -->
        <view class="detail__panel">
          <view class="detail__panel-head">
            <view class="detail__panel-line"></view>
            <text class="detail__panel-title">品录</text>
          </view>
          <view class="detail__rows">
            <view v-for="row in attrs" :key="row.label" class="detail__row">
              <text class="detail__row-label">{{ row.label }}</text>
              <text class="detail__row-value">{{ row.value || '—' }}</text>
            </view>
          </view>
        </view>

        <!-- 描述 -->
        <view v-if="item.description" class="detail__panel">
          <view class="detail__panel-head">
            <view class="detail__panel-line"></view>
            <text class="detail__panel-title">形制</text>
          </view>
          <text class="detail__paragraph">{{ item.description }}</text>
        </view>

        <!-- 故事 -->
        <view v-if="item.story" class="detail__story">
          <text class="detail__story-label">衣 之 所 载</text>
          <text class="detail__story-text">{{ item.story }}</text>
          <view class="u-hairline detail__story-line"></view>
        </view>

        <!-- 相关搭配 -->
        <view v-if="outfits.length" class="detail__related">
          <view class="detail__panel-head">
            <view class="detail__panel-line"></view>
            <text class="detail__panel-title">收录于这些搭配</text>
          </view>
          <scroll-view class="detail__scroll" scroll-x :show-scrollbar="false">
            <view class="detail__scroll-inner">
              <view v-for="o in outfits" :key="o.id" class="detail__scroll-item">
                <InkOutfitCard :outfit="o" />
              </view>
            </view>
          </scroll-view>
        </view>

        <!-- 同类推荐 -->
        <view v-if="related.length" class="detail__related">
          <view class="detail__panel-head">
            <view class="detail__panel-line"></view>
            <text class="detail__panel-title">同气相求</text>
          </view>
          <view class="detail__grid">
            <InkItemCard v-for="r in related" :key="r.id" :item="r" />
          </view>
        </view>
      </view>

      <view class="detail__spacer"></view>

      <!-- 底部操作栏 -->
      <view class="detail__bar">
        <view class="detail__bar-stat">
          <text class="detail__bar-num">{{ item.viewCount || 0 }}</text>
          <text class="detail__bar-label">浏览</text>
        </view>
        <view class="detail__bar-btn u-press" @tap="goCreate">
          <text class="detail__bar-btn-text">以此为材 · 去搭配</text>
        </view>
        <button class="detail__bar-share" open-type="share">
          <text class="detail__bar-share-text">分享</text>
        </button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import { fetchItemDetail } from '@/api'
import { resolveImage } from '@/config'

const id = ref('')
const item = ref(null)
const related = ref([])
const outfits = ref([])
const loading = ref(true)
const error = ref('')

const cover = computed(() => resolveImage(item.value?.imageUrl))

const attrs = computed(() => {
  const it = item.value || {}
  return [
    { label: '部位', value: it.categoryText },
    { label: '稀有度', value: it.rarityText },
    { label: '色系', value: it.colorText },
    { label: '风格', value: it.styleText },
    { label: '系列', value: it.series },
    { label: '适用', value: genderText(it.gender) },
    { label: '获取', value: it.obtainMethod },
    { label: '上线', value: it.releaseDate },
  ]
})

function genderText(g) {
  if (g === 'female') return '女'
  if (g === 'male') return '男'
  return '通用'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await fetchItemDetail(id.value)
    item.value = data.item
    related.value = data.related || []
    outfits.value = data.outfits || []
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function goCreate() {
  uni.navigateTo({ url: `/pages/outfit/create?itemId=${id.value}` })
}

onLoad((options) => {
  id.value = options.id
  load()
})

onShareAppMessage(() => ({
  title: item.value ? `${item.value.name} · 燕云十六声时装图鉴` : '燕云十六声时装图鉴',
  path: `/pages/item/detail?id=${id.value}`,
  imageUrl: cover.value,
}))
</script>

<style lang="scss" scoped>
.detail {
  min-height: 100vh;
  background: $paper-100;

  &__loading {
    padding: $space-32;
  }

  &__hero {
    position: relative;
    width: 100%;
    height: 940rpx;
    overflow: hidden;
    background: $paper-200;
  }

  &__img {
    width: 100%;
    height: 100%;
    display: block;
  }

  &__hero-veil {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 55%;
    background: linear-gradient(180deg, rgba(16, 14, 12, 0) 0%, rgba(16, 14, 12, 0.72) 100%);
  }

  &__hero-text {
    position: absolute;
    left: $space-32;
    right: $space-32;
    bottom: $space-40;
  }

  &__name {
    display: block;
    margin-top: $space-16;
    font-family: $font-display;
    font-size: 56rpx;
    color: #fdfbf7;
    letter-spacing: 6rpx;
    text-shadow: 0 4rpx 18rpx rgba(0, 0, 0, 0.4);
  }

  &__hero-meta {
    display: flex;
    align-items: center;
    gap: 10rpx;
    margin-top: $space-12;
  }

  &__tone {
    width: 16rpx;
    height: 16rpx;
    border-radius: 50%;
    border: 1px solid rgba(253, 251, 247, 0.6);
  }

  &__hero-sub {
    font-size: 22rpx;
    color: rgba(253, 251, 247, 0.8);
    letter-spacing: 2rpx;
  }

  &__glyph-wrap {
    position: absolute;
    right: $space-32;
    top: $space-40;
    width: 88rpx;
    height: 88rpx;
    border: 1px solid rgba(201, 160, 99, 0.75);
    border-radius: 4rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(16, 14, 12, 0.18);
  }

  &__glyph {
    font-family: $font-display;
    font-size: 48rpx;
    color: rgba(253, 251, 247, 0.92);
  }

  &__body {
    padding: $space-32;
  }

  &__quote {
    display: flex;
    gap: 4rpx;
    margin-bottom: $space-24;
  }

  &__quote-mark {
    font-family: $font-display;
    font-size: 36rpx;
    color: $gold;
    line-height: 1.2;
  }

  &__quote-text {
    flex: 1;
    font-size: $text-26;
    color: $ink-600;
    line-height: 1.8;
    letter-spacing: 1rpx;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
    margin-bottom: $space-32;
  }

  &__panel {
    margin-bottom: $space-40;
  }

  &__panel-head {
    display: flex;
    align-items: center;
    gap: $space-16;
    margin-bottom: $space-20;
  }

  &__panel-line {
    width: 6rpx;
    height: 26rpx;
    background: $accent;
    border-radius: 999rpx;
  }

  &__panel-title {
    font-family: $font-display;
    font-size: $text-30;
    color: $ink-900;
    letter-spacing: 4rpx;
  }

  &__rows {
    border-top: 1px solid $ink-100;
  }

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-20 0;
    border-bottom: 1px solid rgba(217, 208, 198, 0.55);
  }

  &__row-label {
    font-size: 24rpx;
    color: $ink-400;
    letter-spacing: 2rpx;
  }

  &__row-value {
    font-size: 24rpx;
    color: $ink-800;
  }

  &__paragraph {
    font-size: $text-26;
    color: $ink-600;
    line-height: 1.9;
    letter-spacing: 0.5rpx;
  }

  &__story {
    position: relative;
    margin: $space-8 0 $space-40;
    padding: $space-32;
    background: $paper-50;
    border: 1px solid $ink-100;
    border-radius: $radius-12;
  }

  &__story-label {
    display: block;
    font-size: 20rpx;
    color: $gold;
    letter-spacing: 8rpx;
    margin-bottom: $space-16;
  }

  &__story-text {
    font-family: $font-display;
    font-size: $text-26;
    color: $ink-700;
    line-height: 2;
    letter-spacing: 1rpx;
  }

  &__story-line {
    margin-top: $space-24;
  }

  &__related {
    margin-bottom: $space-40;
  }

  &__scroll {
    white-space: nowrap;
  }

  &__scroll-inner {
    display: flex;
    gap: $space-20;
  }

  &__scroll-item {
    width: 320rpx;
    flex: 0 0 320rpx;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: $space-24;
  }

  &__spacer {
    height: 160rpx;
  }

  &__bar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 30;
    display: flex;
    align-items: center;
    gap: $space-20;
    padding: $space-16 $space-32;
    padding-bottom: calc(#{$space-16} + env(safe-area-inset-bottom));
    background: rgba(253, 251, 247, 0.96);
    border-top: 1px solid $ink-100;
    backdrop-filter: blur(8px);
  }

  &__bar-stat {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 72rpx;
  }

  &__bar-num {
    font-family: $font-display;
    font-size: 28rpx;
    color: $ink-800;
    line-height: 1.1;
  }

  &__bar-label {
    font-size: 18rpx;
    color: $ink-300;
  }

  &__bar-btn {
    flex: 1;
    height: 84rpx;
    border-radius: $radius-8;
    background: $accent;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__bar-btn-text {
    font-size: $text-28;
    color: #fdfbf7;
    letter-spacing: 4rpx;
  }

  &__bar-share {
    width: 100rpx;
    height: 84rpx;
    border-radius: $radius-8;
    border: 1px solid $ink-200;
    background: transparent;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    margin: 0;
    line-height: 1;
  }

  &__bar-share-text {
    font-size: 24rpx;
    color: $ink-600;
    letter-spacing: 2rpx;
  }
}
</style>
