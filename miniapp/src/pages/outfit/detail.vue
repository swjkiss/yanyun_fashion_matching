<template>
  <view class="od">
    <view v-if="loading" class="od__loading">
      <InkSkeleton cover-height="820rpx" />
    </view>

    <InkEmpty v-else-if="error" title="加载失败" :hint="error" action-text="重试" @action="load" />

    <template v-else-if="outfit">
      <view class="od__hero">
        <image class="od__cover" :src="cover" mode="aspectFill" @tap="previewCover" />
        <view class="od__veil"></view>
        <view class="od__hero-text">
          <text class="od__scene">{{ outfit.sceneTag || outfit.styleText }}</text>
          <text class="od__title">{{ outfit.title }}</text>
          <text v-if="outfit.subtitle" class="od__subtitle">{{ outfit.subtitle }}</text>
        </view>
      </view>

      <view class="od__body">
        <!-- 作者 -->
        <view class="od__author">
          <image class="od__avatar" :src="avatar" mode="aspectFill" />
          <view class="od__author-main">
            <text class="od__author-name">{{ outfit.authorName }}</text>
            <text class="od__author-sub">{{ outfit.styleText }} · {{ outfit.itemCount }} 件成衣</text>
          </view>
          <view class="od__stats">
            <text class="od__stat-num">{{ outfit.viewCount || 0 }}</text>
            <text class="od__stat-label">浏览</text>
          </view>
        </view>

        <!-- 描述 -->
        <text v-if="outfit.description" class="od__desc">{{ outfit.description }}</text>

        <!-- 组成时装 -->
        <view class="od__section">
          <view class="od__section-head">
            <view class="od__section-line"></view>
            <text class="od__section-title">一套行头</text>
            <text class="od__section-count">{{ items.length }} 件</text>
          </view>
          <view class="od__items">
            <view v-for="it in items" :key="it.id" class="od__item u-press" @tap="goItem(it)">
              <view class="od__item-media">
                <image class="od__item-img" :src="img(it.imageUrl)" mode="aspectFill" lazy-load />
              </view>
              <view class="od__item-body">
                <text class="od__item-name u-ellipsis">{{ it.name }}</text>
                <view class="od__item-meta">
                  <InkRarity :value="it.rarity" :text="it.rarityText" :color="it.rarityColor" />
                  <text class="od__item-cat">{{ it.categoryText }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view class="od__foot">
          <view class="u-hairline od__foot-line"></view>
          <text class="od__foot-text">衣 以 载 道</text>
        </view>
      </view>

      <view class="od__spacer"></view>

      <!-- 底部操作 -->
      <view class="od__bar">
        <view class="od__bar-btn u-press" :class="{ 'od__bar-btn--on': favorited }" @tap="onFav">
          <text class="od__bar-icon">{{ favorited ? '★' : '☆' }}</text>
          <text class="od__bar-text">{{ favorited ? '已收藏' : '收藏' }}</text>
        </view>
        <view class="od__bar-btn u-press" @tap="onLike">
          <text class="od__bar-icon">♡</text>
          <text class="od__bar-text">{{ likeCount }}</text>
        </view>
        <view class="od__bar-btn u-press" @tap="makePoster">
          <text class="od__bar-icon">⊞</text>
          <text class="od__bar-text">海报</text>
        </view>
        <button class="od__bar-main" open-type="share">
          <text class="od__bar-main-text">分享此搭配</text>
        </button>
      </view>

      <!-- 海报画布（离屏隐藏） -->
      <canvas
        id="poster"
        type="2d"
        class="od__poster-canvas"
        :style="{ width: posterW + 'px', height: posterH + 'px' }"
      ></canvas>
    </template>
  </view>
</template>

<script setup>
import { computed, getCurrentInstance, ref } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import { fetchOutfitDetail, likeOutfit, toggleFavorite } from '@/api'
import { resolveImage } from '@/config'

const instance = getCurrentInstance()
const id = ref('')
const data = ref(null)
const loading = ref(true)
const error = ref('')
const favorited = ref(false)
const likeCount = ref(0)
const posterW = ref(300)
const posterH = ref(534)

const outfit = computed(() => data.value?.outfit || null)
const items = computed(() => data.value?.itemDetails || [])
const cover = computed(() => resolveImage(outfit.value?.coverImage))
const avatar = computed(() => resolveImage(outfit.value?.authorAvatar))

const img = (p) => resolveImage(p)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchOutfitDetail(id.value)
    data.value = result
    favorited.value = !!result.favorited
    likeCount.value = result.outfit?.likeCount || 0
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function onFav() {
  try {
    const result = await toggleFavorite(id.value)
    favorited.value = result.favorited
    if (data.value?.outfit) data.value.outfit.favCount = result.favCount
    uni.showToast({ title: favorited.value ? '已收入匣中' : '已取消收藏', icon: 'none' })
  } catch (e) {
    /* request 层已提示 */
  }
}

async function onLike() {
  try {
    const result = await likeOutfit(id.value)
    likeCount.value = result.likeCount
    uni.showToast({ title: '已点赞', icon: 'none' })
  } catch (e) {
    /* ignore */
  }
}

function goItem(it) {
  uni.navigateTo({ url: `/pages/item/detail?id=${it.id}` })
}
function previewCover() {
  if (cover.value) uni.previewImage({ urls: [cover.value] })
}

/** 生成分享海报：宣纸底 + 封面 + 标题 + 竖排小字 + 金线框。 */
function makePoster() {
  // #ifdef H5
  uni.showToast({ title: '请在小程序内长按保存海报', icon: 'none' })
  return
  // #endif
  // #ifndef H5
  const query = uni.createSelectorQuery().in(instance.proxy)
  query
    .select('#poster')
    .fields({ node: true, size: true })
    .exec(async (res) => {
      const node = res && res[0] && res[0].node
      if (!node) {
        uni.showToast({ title: '海报生成失败', icon: 'none' })
        return
      }
      try {
        const dpr = uni.getSystemInfoSync().pixelRatio || 2
        const W = 750
        const H = 1334
        node.width = W * dpr
        node.height = H * dpr
        const ctx = node.getContext('2d')
        ctx.scale(dpr, dpr)
        ctx.fillStyle = '#F7F2E9'
        ctx.fillRect(0, 0, W, H)

        // 封面
        const image = node.createImage()
        await new Promise((resolve) => {
          image.onload = resolve
          image.onerror = resolve
          image.src = cover.value
        })
        try {
          ctx.drawImage(image, 48, 96, W - 96, 880)
        } catch (e) {
          ctx.fillStyle = '#EFE7D9'
          ctx.fillRect(48, 96, W - 96, 880)
        }

        // 底部压暗
        const grad = ctx.createLinearGradient(0, 640, 0, 976)
        grad.addColorStop(0, 'rgba(16,14,12,0)')
        grad.addColorStop(1, 'rgba(16,14,12,0.66)')
        ctx.fillStyle = grad
        ctx.fillRect(48, 640, W - 96, 336)

        // 标题
        ctx.fillStyle = '#FDFBF7'
        ctx.font = '44px serif'
        ctx.fillText(outfit.value.title || '', 88, 900)
        ctx.font = '22px sans-serif'
        ctx.fillStyle = 'rgba(253,251,247,0.82)'
        ctx.fillText(outfit.value.subtitle || outfit.value.sceneTag || '', 88, 940)

        // 金线框
        ctx.strokeStyle = 'rgba(201,160,99,0.9)'
        ctx.lineWidth = 2
        ctx.strokeRect(28, 28, W - 56, H - 56)

        // 竖排小字
        ctx.fillStyle = '#B08A45'
        ctx.font = '24px serif'
        const seal = '燕云十六声'
        for (let i = 0; i < seal.length; i++) {
          ctx.fillText(seal[i], W - 88, 108 + i * 34)
        }

        // 作者
        ctx.fillStyle = '#514840'
        ctx.font = '22px sans-serif'
        ctx.fillText(`— ${outfit.value.authorName || '云游客'}`, 88, H - 120)

        uni.showLoading({ title: '生成中' })
        setTimeout(() => {
          uni.canvasToTempFilePath({
            canvas: node,
            success: (r) => {
              uni.hideLoading()
              uni.previewImage({ urls: [r.tempFilePath] })
            },
            fail: () => {
              uni.hideLoading()
              uni.showToast({ title: '保存失败', icon: 'none' })
            },
          })
        }, 220)
      } catch (e) {
        uni.showToast({ title: '海报生成失败', icon: 'none' })
      }
    })
  // #endif
}

onLoad((options) => {
  id.value = options.id
  load()
})

onShareAppMessage(() => ({
  title: outfit.value ? `${outfit.value.title} · 燕云十六声搭配` : '燕云十六声搭配',
  path: `/pages/outfit/detail?id=${id.value}`,
  imageUrl: cover.value,
}))
</script>

<style lang="scss" scoped>
.od {
  min-height: 100vh;
  background: $paper-100;

  &__loading {
    padding: $space-32;
  }

  &__hero {
    position: relative;
    height: 820rpx;
    overflow: hidden;
    background: $paper-200;
  }

  &__cover {
    width: 100%;
    height: 100%;
    display: block;
  }

  &__veil {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 60%;
    background: linear-gradient(180deg, rgba(16, 14, 12, 0) 0%, rgba(16, 14, 12, 0.74) 100%);
  }

  &__hero-text {
    position: absolute;
    left: $space-32;
    right: $space-32;
    bottom: $space-40;
  }

  &__scene {
    display: inline-block;
    font-size: 20rpx;
    color: rgba(253, 251, 247, 0.9);
    letter-spacing: 6rpx;
    border-left: 3rpx solid $gold;
    padding-left: 12rpx;
  }

  &__title {
    display: block;
    margin-top: $space-16;
    font-family: $font-display;
    font-size: 56rpx;
    color: #fdfbf7;
    letter-spacing: 8rpx;
    text-shadow: 0 4rpx 18rpx rgba(0, 0, 0, 0.4);
  }

  &__subtitle {
    display: block;
    margin-top: $space-12;
    font-size: 24rpx;
    color: rgba(253, 251, 247, 0.82);
    letter-spacing: 2rpx;
  }

  &__body {
    padding: $space-32;
  }

  &__author {
    display: flex;
    align-items: center;
    gap: $space-16;
    padding-bottom: $space-24;
    border-bottom: 1px solid $ink-100;
  }

  &__avatar {
    width: 72rpx;
    height: 72rpx;
    border-radius: 50%;
    background: $paper-300;
  }

  &__author-main {
    flex: 1;
    min-width: 0;
  }

  &__author-name {
    display: block;
    font-size: $text-28;
    color: $ink-900;
  }

  &__author-sub {
    display: block;
    margin-top: 4rpx;
    font-size: 20rpx;
    color: $ink-300;
    letter-spacing: 1rpx;
  }

  &__stats {
    text-align: right;
  }

  &__stat-num {
    display: block;
    font-family: $font-display;
    font-size: 30rpx;
    color: $ink-800;
    line-height: 1.1;
  }

  &__stat-label {
    font-size: 18rpx;
    color: $ink-300;
  }

  &__desc {
    display: block;
    margin: $space-24 0;
    font-size: $text-26;
    color: $ink-600;
    line-height: 1.9;
  }

  &__section {
    margin-top: $space-40;
  }

  &__section-head {
    display: flex;
    align-items: center;
    gap: $space-16;
    margin-bottom: $space-20;
  }

  &__section-line {
    width: 6rpx;
    height: 26rpx;
    background: $accent;
    border-radius: 999rpx;
  }

  &__section-title {
    flex: 1;
    font-family: $font-display;
    font-size: $text-30;
    color: $ink-900;
    letter-spacing: 4rpx;
  }

  &__section-count {
    font-size: 20rpx;
    color: $ink-300;
  }

  &__items {
    display: flex;
    flex-direction: column;
    gap: $space-16;
  }

  &__item {
    display: flex;
    gap: $space-20;
    padding: $space-16;
    background: $paper-50;
    border: 1px solid $ink-100;
    border-radius: $radius-12;
  }

  &__item-media {
    width: 120rpx;
    height: 160rpx;
    border-radius: $radius-8;
    overflow: hidden;
    background: $paper-200;
    flex: 0 0 120rpx;
  }

  &__item-img {
    width: 100%;
    height: 100%;
    display: block;
  }

  &__item-body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 12rpx;
  }

  &__item-name {
    font-family: $font-display;
    font-size: $text-28;
    color: $ink-900;
    letter-spacing: 1rpx;
  }

  &__item-meta {
    display: flex;
    align-items: center;
    gap: 10rpx;
  }

  &__item-cat {
    font-size: 20rpx;
    color: $ink-400;
  }

  &__foot {
    margin-top: $space-56;
  }

  &__foot-line {
    margin-bottom: $space-20;
  }

  &__foot-text {
    display: block;
    text-align: center;
    font-size: 20rpx;
    color: $ink-300;
    letter-spacing: 8rpx;
  }

  &__spacer {
    height: 170rpx;
  }

  &__bar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 30;
    display: flex;
    align-items: center;
    gap: $space-12;
    padding: $space-16 $space-24;
    padding-bottom: calc(#{$space-16} + env(safe-area-inset-bottom));
    background: rgba(253, 251, 247, 0.97);
    border-top: 1px solid $ink-100;
  }

  &__bar-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 96rpx;
    height: 84rpx;
    border-radius: $radius-8;
    border: 1px solid $ink-100;

    &--on {
      border-color: rgba(166, 50, 42, 0.4);
      background: rgba(166, 50, 42, 0.06);
    }

    &--on .od__bar-icon,
    &--on .od__bar-text {
      color: $accent;
    }
  }

  &__bar-icon {
    font-size: 26rpx;
    color: $ink-600;
    line-height: 1.1;
  }

  &__bar-text {
    margin-top: 2rpx;
    font-size: 18rpx;
    color: $ink-400;
  }

  &__bar-main {
    flex: 1;
    height: 84rpx;
    border-radius: $radius-8;
    background: $accent;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    margin: 0;
    line-height: 1;
  }

  &__bar-main-text {
    font-size: $text-28;
    color: #fdfbf7;
    letter-spacing: 4rpx;
  }

  &__poster-canvas {
    position: fixed;
    left: -9999px;
    top: 0;
  }
}
</style>
