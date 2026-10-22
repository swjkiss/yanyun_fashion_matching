<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">收藏记录</h2>
        <p class="page-sub">用户收藏行为明细，可删除异常数据。</p>
      </div>
    </div>

    <div class="panel">
      <div class="filter-row">
        <a-input v-model:value="filters.outfitTitle" placeholder="搭配标题" allow-clear style="width: 200px" @press-enter="reload" />
        <a-input v-model:value="filters.userName" placeholder="用户昵称" allow-clear style="width: 160px" @press-enter="reload" />
        <a-button type="primary" @click="reload">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'cover'">
            <a-image :src="imgUrl(record.coverImage)" :width="64" :height="46" style="object-fit: cover; border-radius: 6px" />
          </template>
          <template v-else-if="column.key === 'outfitTitle'">
            <span class="cell-name"><b>{{ record.outfitTitle }}</b></span>
          </template>
          <template v-else-if="column.key === 'userName'">
            <span class="row-tight">
              <a-avatar :size="22" style="background: var(--gold); font-size: 11px">{{ (record.userName || '匿')[0] }}</a-avatar>
              {{ record.userName || '匿名用户' }}
            </span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-popconfirm title="确认删除这条收藏记录？" ok-text="删除" @confirm="onRemove(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { favoriteApi, type FavoriteRow, type PageData } from '../../api'
import { errText, imgUrl, formatDateTime } from '../../utils/format'

const loading = ref(false)
const rows = ref<FavoriteRow[]>([])
const filters = reactive({ outfitTitle: '', userName: '' })

const columns = [
  { title: '封面', key: 'cover', width: 92 },
  { title: '搭配', key: 'outfitTitle' },
  { title: '用户', key: 'userName', width: 160 },
  { title: '收藏时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'actions', width: 80 },
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
})

async function load() {
  loading.value = true
  try {
    const q: Record<string, unknown> = { pageNo: pagination.current, pageSize: pagination.pageSize }
    if (filters.outfitTitle) q.outfitTitle = filters.outfitTitle
    if (filters.userName) q.userName = filters.userName
    const data: PageData<FavoriteRow> = await favoriteApi.list(q)
    rows.value = (data.records || []).map((r) => ({ ...r, createTime: formatDateTime(r.createTime) }))
    pagination.total = data.total || 0
  } catch (e) {
    message.error(errText(e))
  } finally {
    loading.value = false
  }
}

function reload() {
  pagination.current = 1
  load()
}

function onTableChange(p: { current: number; pageSize: number }) {
  pagination.current = p.current
  pagination.pageSize = p.pageSize
  load()
}

function resetFilters() {
  filters.outfitTitle = ''
  filters.userName = ''
  reload()
}

function onRemove(row: FavoriteRow) {
  favoriteApi
    .remove(row.id)
    .then(() => {
      message.success('已删除')
      load()
    })
    .catch((e) => message.error(errText(e)))
}

onMounted(load)
</script>

<style scoped>
.panel {
  background: var(--surface-raised);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px 18px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.cell-name b {
  font-size: 13px;
}
</style>
