<template>
  <view class="home">
    <!-- 沉浸式头部 -->
    <view class="home__status" :style="{ height: statusHeight + 'px' }"></view>
    <view class="home__brand">
      <view class="home__seal">
        <text class="home__seal-text">燕</text>
      </view>
      <view class="home__brand-main">
        <text class="home__brand-title">燕云十六声</text>
        <text class="home__brand-sub">FASHION ARCHIVE · 时装图鉴</text>
      </view>
      <view class="home__brand-right u-press" @tap="goMine">
        <text class="home__brand-count">{{ stats.itemTotal || 0 }}</text>
        <text class="home__brand-count-label">件在录</text>
      </view>
    </view>

    <!-- 搜索 -->
    <view class="home__search u-press" @tap="goSearch">
      <text class="home__search-icon">⌕</text>
      <text class="home__search-ph">搜索时装 · 系列 · 标签</text>
    </view>

    <!-- 骨架屏 -->
    <view v-if="loading" class="home__loading">
      <InkSkeleton cover-height="480rpx" />
      <view class="home__grid">
        <InkSkeleton cover-height="360rpx" />
        <InkSkeleton cover-height="360rpx" />
      </view>
    </view>

    <!-- 错误态 -->
    <InkEmpty
      v-else-if="error"
      title="没能连上服务器"
      :hint="error"
      action-text="重新加载"
      @action="load()"
    />

    <template v-else>
      <!-- 主视觉 -->
      <view class="home__banner" @tap="goSearch">
        <image class="home__banner-img" :src="bannerImage" mode="aspectFill" />
        <view class="home__banner-veil"></view>
        <view class="home__banner-text">
          <text class="home__banner-title">{{ home.banner?.title || '云想衣裳' }}</text>
          <text class="home__banner-sub">{{ home.banner?.subtitle || '十六声 · 时装图鉴' }}</text>
        </view>
        <view class="home__banner-hairline"></view>
      </view>

      <!-- 部位入口 -->
      <view class="home__cats">
        <view
          v-for="cat in categories"
          :key="cat.value"
          class="home__cat u-press"
          @tap="goCategory(cat)"
        >
          <text class="home__cat-name">{{ cat.text }}</text>
          <text class="home__cat-count">{{ cat.count }}</text>
        </view>
      </view>

      <!-- 今日推荐 -->
      <view class="home__section">
        <view class="home__section-head">
          <view class="home__section-line"></view>
          <text class="home__section-title">今日推荐</text>
          <text class="home__section-more" @tap="goSearch">更多 ›</text>
        </view>
        <scroll-view class="home__scroll" scroll-x :show-scrollbar="false" enable-flex>
          <view class="home__scroll-inner">
            <view v-for="item in home.recommend" :key="item.id" class="home__scroll-item">
              <InkItemCard :item="item" />
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 热门搭配 -->
      <view class="home__section">
        <view class="home__section-head">
          <view class="home__section-line"></view>
          <text class="home__section-title">热门搭配</text>
          <text class="home__section-more" @tap="goOutfits">全部 ›</text>
        </view>
        <view class="home__grid">
          <InkOutfitCard v-for="o in home.hotOutfits" :key="o.id" :outfit="o" />
        </view>
        <InkEmpty v-if="!home.hotOutfits || !home.hotOutfits.length" title="还没有搭配" hint="去创建第一套吧" />
      </view>

      <view class="home__foot">
        <view class="u-hairline home__foot-line"></view>
        <text class="home__foot-text">墨 · 纸 · 金 · 朱砂</text>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShareAppMessage } from '@dcloudio/uni-app'
import { fetchHome } from '@/api'
import { resolveImage, statusBarHeight } from '@/config'

const statusHeight = ref(statusBarHeight())
const loading = ref(true)
const error = ref('')
const home = ref({ banner: {}, stats: {}, categories: [], recommend: [], hotOutfits: [] })

const stats = computed(() => home.value.stats || {})
const categories = computed(() => (home.value.categories || []).slice(0, 6))
const bannerImage = computed(() =>
  resolveImage(home.value.banner?.image) || resolveImage('/images/hero-banner.jpg')
)

async function load() {
  error.value = ''
  try {
    home.value = await fetchHome()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function goSearch() {
  uni.navigateTo({ url: '/pages/search/search' })
}
function goCategory(cat) {
  uni.navigateTo({ url: `/pages/search/search?category=${cat.value}&categoryText=${cat.text}` })
}
function goOutfits() {
  uni.switchTab({ url: '/pages/outfit/index' })
}
function goMine() {
  uni.switchTab({ url: '/pages/mine/index' })
}

onLoad(load)
onPullDownRefresh(() => load())
onShareAppMessage(() => ({
  title: '燕云十六声 · 时装图鉴',
  path: '/pages/index/index',
  imageUrl: bannerImage.value,
}))
</script>

<style lang="scss" scoped>
.home {
  min-height: 100vh;
  background: $paper-100;
  padding-bottom: $space-48;

  &__status {
    width: 100%;
  }

  /* ---------- 品牌头 ---------- */
  &__brand {
    display: flex;
    align-items: center;
    gap: $space-16;
    padding: $space-16 $space-32 $space-8;
  }

  &__seal {
    width: 56rpx;
    height: 56rpx;
    border-radius: 6rpx;
    background: $accent;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__seal-text {
    font-family: $font-display;
    font-size: 34rpx;
    color: #fdfbf7;
  }

  &__brand-main {
    flex: 1;
    min-width: 0;
  }

  &__brand-title {
    display: block;
    font-family: $font-display;
    font-size: 40rpx;
    letter-spacing: 6rpx;
    color: $ink-900;
  }

  &__brand-sub {
    display: block;
    margin-top: 2rpx;
    font-size: 18rpx;
    letter-spacing: 2rpx;
    color: $ink-300;
  }

  &__brand-right {
    text-align: right;
  }

  &__brand-count {
    display: block;
    font-family: $font-display;
    font-size: 36rpx;
    color: $accent;
    line-height: 1.1;
  }

  &__brand-count-label {
    font-size: 18rpx;
    color: $ink-300;
  }

  /* ---------- 搜索 ---------- */
  &__search {
    margin: $space-16 $space-32 0;
    height: 76rpx;
    border-radius: $radius-8;
    background: $paper-50;
    border: 1px solid $ink-100;
    display: flex;
    align-items: center;
    padding: 0 $space-24;
    gap: $space-12;
  }

  &__search-icon {
    font-size: 30rpx;
    color: $ink-300;
  }

  &__search-ph {
    font-size: $text-26;
    color: $ink-300;
    letter-spacing: 1rpx;
  }

  /* ---------- Banner ---------- */
  &__banner {
    position: relative;
    margin: $space-24 $space-32 0;
    height: 420rpx;
    border-radius: $radius-16;
    overflow: hidden;
    box-shadow: $shadow-md;
  }

  &__banner-img {
    width: 100%;
    height: 100%;
    display: block;
  }

  &__banner-veil {
    position: absolute;
    inset: 0;
    background: linear-gradient(
      90deg,
      rgba(16, 14, 12, 0.55) 0%,
      rgba(16, 14, 12, 0.18) 52%,
      rgba(16, 14, 12, 0) 100%
    );
  }

  &__banner-text {
    position: absolute;
    left: $space-32;
    top: 50%;
    transform: translateY(-50%);
  }

  &__banner-title {
    display: block;
    font-family: $font-display;
    font-size: 60rpx;
    color: #fdfbf7;
    letter-spacing: 12rpx;
    text-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.4);
  }

  &__banner-sub {
    display: block;
    margin-top: $space-12;
    font-size: 22rpx;
    color: rgba(253, 251, 247, 0.82);
    letter-spacing: 4rpx;
  }

  &__banner-hairline {
    position: absolute;
    left: $space-32;
    bottom: 48rpx;
    width: 96rpx;
    height: 2rpx;
    background: rgba(201, 160, 99, 0.85);
  }

  /* ---------- 部位入口 ---------- */
  &__cats {
    margin: $space-24 $space-32 0;
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
  }

  &__cat {
    flex: 1 1 30%;
    min-width: 180rpx;
    height: 108rpx;
    border-radius: $radius-8;
    background: $paper-50;
    border: 1px solid $ink-100;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4rpx;
  }

  &__cat-name {
    font-family: $font-display;
    font-size: $text-28;
    color: $ink-800;
    letter-spacing: 3rpx;
  }

  &__cat-count {
    font-size: 18rpx;
    color: $ink-300;
  }

  /* ---------- 区块 ---------- */
  &__section {
    margin-top: $space-48;
  }

  &__section-head {
    display: flex;
    align-items: center;
    padding: 0 $space-32;
    margin-bottom: $space-20;
    gap: $space-16;
  }

  &__section-line {
    width: 6rpx;
    height: 28rpx;
    background: $accent;
    border-radius: 999rpx;
  }

  &__section-title {
    flex: 1;
    font-family: $font-display;
    font-size: $text-34;
    color: $ink-900;
    letter-spacing: 4rpx;
  }

  &__section-more {
    font-size: 22rpx;
    color: $ink-300;
  }

  &__scroll {
    width: 100%;
    white-space: nowrap;
  }

  &__scroll-inner {
    display: flex;
    gap: $space-20;
    padding: 0 $space-32;
  }

  &__scroll-item {
    width: 300rpx;
    flex: 0 0 300rpx;
  }

  &__grid {
    padding: 0 $space-32;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: $space-24;
  }

  &__loading {
    padding: $space-32;
  }

  &__foot {
    margin-top: $space-72;
    padding: 0 $space-32;
    text-align: center;
  }

  &__foot-line {
    margin-bottom: $space-24;
  }

  &__foot-text {
    font-size: 20rpx;
    color: $ink-300;
    letter-spacing: 8rpx;
  }
}
</style>
