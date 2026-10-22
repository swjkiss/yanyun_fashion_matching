<template>
  <div class="dash">
    <!-- 概览四卡 -->
    <div class="dash__cards">
      <div v-for="c in cards" :key="c.label" class="dash__card">
        <div class="dash__card-head">
          <span class="dash__card-label">{{ c.label }}</span>
          <span class="dash__card-icon"><component :is="c.icon" :size="16" :stroke-width="1.8" /></span>
        </div>
        <div class="dash__card-value num">{{ overview ? c.value() : '—' }}</div>
        <div class="dash__card-foot">{{ c.foot }}</div>
      </div>
    </div>

    <div class="dash__grid">
      <!-- 近 7 日趋势 -->
      <div class="dash__panel dash__panel--wide">
        <div class="dash__panel-head">
          <h3>近 7 日新增</h3>
          <span class="dash__legend"><i class="dot dot--ink" />时装<i class="dot dot--accent" />搭配<i class="dot dot--gold" />收藏</span>
        </div>
        <a-skeleton v-if="loading" active :paragraph="{ rows: 4 }" />
        <template v-else>
          <div class="dash__bars">
            <div v-for="row in trend" :key="row.date" class="dash__bar-group">
              <div class="dash__bars-stack">
                <div
                  v-for="(seg, i) in barSegs(row)"
                  :key="i"
                  class="dash__bar-seg"
                  :class="seg.cls"
                  :style="{ height: seg.h + 'px' }"
                  :title="`${seg.label} ${seg.value}`"
                />
              </div>
              <span class="dash__bar-date">{{ row.date.slice(5) }}</span>
            </div>
          </div>
          <a-empty v-if="!trend.length" description="暂无趋势数据" />
        </template>
      </div>

      <!-- 部位分布 -->
      <div class="dash__panel">
        <div class="dash__panel-head"><h3>部位分布</h3></div>
        <a-skeleton v-if="loading" active :paragraph="{ rows: 5 }" />
        <ul v-else class="dash__dist">
          <li v-for="row in overview?.categoryDist || []" :key="row.name" class="dash__dist-row">
            <span class="dash__dist-name">{{ row.name }}</span>
            <span class="dash__dist-track">
              <span class="dash__dist-fill" :style="{ width: pct(row.value, overview?.categoryDist) + '%' }" />
            </span>
            <span class="dash__dist-val num">{{ row.value }}</span>
          </li>
        </ul>
      </div>

      <!-- 稀有度分布 -->
      <div class="dash__panel">
        <div class="dash__panel-head"><h3>稀有度分布</h3></div>
        <a-skeleton v-if="loading" active :paragraph="{ rows: 5 }" />
        <ul v-else class="dash__dist">
          <li v-for="row in overview?.rarityDist || []" :key="row.name" class="dash__dist-row">
            <span class="dash__dist-name">{{ row.name }}</span>
            <span class="dash__dist-track">
              <span
                class="dash__dist-fill"
                :style="{ width: pct(row.value, overview?.rarityDist) + '%', background: rarityColorByText(row.name) }"
              />
            </span>
            <span class="dash__dist-val num">{{ row.value }}</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- 热门搭配 -->
    <div class="dash__panel">
      <div class="dash__panel-head">
        <h3>收藏 Top 5 搭配</h3>
      </div>
      <a-table
        :columns="topColumns"
        :data-source="overview?.topOutfits || []"
        :pagination="false"
        :loading="loading"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'cover'">
            <a-image :src="record.coverImage" :width="64" :height="64" style="object-fit: cover; border-radius: 6px" />
          </template>
          <template v-else-if="column.key === 'favCount'">
            <span class="num">{{ record.favCount }}</span>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { Shirt, Sparkles, HeartHandshake, Eye } from 'lucide-vue-next'
import { statsApi, type Overview } from '../../api'
import { rarityColorByText, formatNumber } from '../../utils'
import { dictText } from '../../utils/dict'

const loading = ref(true)
const overview = ref<Overview | null>(null)
const trend = ref<{ date: string; items: number; outfits: number; favorites: number }[]>([])

const cards = computed(() => [
  { label: '时装总数', value: () => formatNumber(overview.value?.itemTotal), foot: `上架 ${overview.value?.itemOnline ?? '—'} 件`, icon: Shirt },
  { label: '搭配方案', value: () => formatNumber(overview.value?.outfitTotal), foot: `待审核 ${overview.value?.outfitPending ?? '—'} 套`, icon: Sparkles },
  { label: '收藏次数', value: () => formatNumber(overview.value?.favoriteTotal), foot: '全部用户收藏', icon: HeartHandshake },
  { label: '浏览总量', value: () => formatNumber(overview.value?.viewTotal), foot: '图鉴 + 搭配页', icon: Eye },
])

const topColumns = [
  { title: '封面', key: 'cover', width: 88 },
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '收藏', key: 'favCount', width: 96 },
]

/** 单行三色堆叠柱：按最大值归一化到 120px */
function barSegs(row: { items: number; outfits: number; favorites: number }) {
  const max = Math.max(1, ...trend.value.map((r) => Math.max(r.items, r.outfits, r.favorites)))
  const mk = (value: number, cls: string, label: string) => ({
    value,
    label,
    cls,
    h: Math.round((value / max) * 96) + 2,
  })
  return [mk(row.favorites, 'is-gold', '收藏'), mk(row.outfits, 'is-accent', '搭配'), mk(row.items, 'is-ink', '时装')]
}

function pct(value: number, list?: { name: string; value: number }[]): number {
  const max = Math.max(1, ...(list || []).map((r) => r.value))
  return Math.round((value / max) * 100)
}

onMounted(async () => {
  try {
    const [ov, td] = await Promise.all([statsApi.overview(), statsApi.trend()])
    overview.value = ov
    trend.value = td || []
  } catch (e) {
    message.error((e as Error).message)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dash__cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.dash__card {
  padding: 16px 18px;
  background: var(--surface-raised);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
}

.dash__card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dash__card-label {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  letter-spacing: 2px;
}

.dash__card-icon {
  color: var(--gold);
}

.dash__card-value {
  margin: 8px 0 4px;
  font-size: 30px;
  font-weight: var(--weight-bold);
  color: var(--text-primary);
  line-height: 1;
}

.dash__card-foot {
  font-size: var(--text-xs);
  color: var(--text-placeholder);
}

.dash__grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.dash__panel {
  padding: 16px 18px;
  background: var(--surface-raised);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-4);
}

.dash__panel--wide {
  margin-bottom: 0;
}

.dash__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.dash__panel-head h3 {
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 3px;
  color: var(--text-primary);
}

.dash__legend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 2px;
  margin: 0 4px 0 10px;
}

.dot--ink { background: var(--color-ink-600); }
.dot--accent { background: var(--accent); }
.dot--gold { background: var(--gold); }

.dash__bars {
  display: flex;
  align-items: flex-end;
  gap: 26px;
  height: 150px;
  padding-top: 6px;
}

.dash__bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.dash__bars-stack {
  display: flex;
  flex-direction: column-reverse;
  gap: 3px;
}

.dash__bar-seg {
  width: 34px;
  min-height: 2px;
  border-radius: 3px;
}

.dash__bar-seg.is-ink { background: var(--color-ink-600); }
.dash__bar-seg.is-accent { background: var(--accent); }
.dash__bar-seg.is-gold { background: var(--gold); }

.dash__bar-date {
  font-size: 11px;
  color: var(--text-placeholder);
}

.dash__dist {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dash__dist-row {
  display: grid;
  grid-template-columns: 52px 1fr 36px;
  align-items: center;
  gap: 10px;
}

.dash__dist-name {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.dash__dist-track {
  height: 8px;
  background: var(--color-paper-200);
  border-radius: var(--radius-pill);
  overflow: hidden;
}

.dash__dist-fill {
  display: block;
  height: 100%;
  background: var(--color-ink-600);
  border-radius: var(--radius-pill);
  transition: width var(--motion-slow) var(--motion-ease);
}

.dash__dist-val {
  font-size: var(--text-sm);
  color: var(--text-primary);
  text-align: right;
}
</style>
