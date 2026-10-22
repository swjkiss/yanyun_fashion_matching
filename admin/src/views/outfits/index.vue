<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">搭配方案</h2>
        <p class="page-sub">用户共创与官方方案的审核、编辑与上下架。</p>
      </div>
      <a-button type="primary" @click="openEditor()">
        <template #icon><plus-outlined /></template>
        新增搭配
      </a-button>
    </div>

    <div class="panel">
      <div class="filter-row">
        <a-input v-model:value="filters.title" placeholder="标题" allow-clear style="width: 220px" @press-enter="reload" />
        <a-select v-model:value="filters.status" placeholder="状态" allow-clear style="width: 130px">
          <a-select-option v-for="o in outfitStatusOptions" :key="o.value" :value="o.value">{{ o.text }}</a-select-option>
        </a-select>
        <a-select v-model:value="filters.styleTag" placeholder="风格" allow-clear style="width: 130px">
          <a-select-option v-for="d in builtinDicts.fashion_style" :key="d.value" :value="d.value">{{ d.text }}</a-select-option>
        </a-select>
        <a-button type="primary" @click="reload">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
        <span class="grow" />
        <a-popconfirm title="确认批量删除选中的搭配？" ok-text="删除" :disabled="!selectedRowKeys.length" @confirm="onRemoveBatch">
          <a-button danger :disabled="!selectedRowKeys.length">批量删除 ({{ selectedRowKeys.length }})</a-button>
        </a-popconfirm>
      </div>

      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :row-selection="{ selectedRowKeys, onChange: (keys: any[]) => (selectedRowKeys = keys) }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'cover'">
            <a-image :src="imgUrl(record.coverImage)" :width="64" :height="46" style="object-fit: cover; border-radius: 6px" />
          </template>
          <template v-else-if="column.key === 'title'">
            <div class="cell-name">
              <b>{{ record.title }}</b>
              <span class="tiny muted ellipsis" style="max-width: 220px">{{ record.subtitle }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'author'">
            <span class="tiny">{{ record.authorName || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'style'">
            <span class="tag tag--soft">{{ dictText('fashion_style', record.styleTag) }}</span>
          </template>
          <template v-else-if="column.key === 'count'">
            <span class="num">{{ record.itemCount }}</span>
          </template>
          <template v-else-if="column.key === 'stats'">
            <span class="tiny muted">藏 {{ formatNumber(record.favCount) }} · 赞 {{ formatNumber(record.likeCount) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <span class="status-pill" :style="{ color: m.color, background: m.bg }" v-for="m in [outfitStatusMeta(record.status)]" :key="m.text">
              {{ m.text }}
            </span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="openEditor(record)">编辑</a-button>
            <a-dropdown>
              <a-button type="link" size="small">审核</a-button>
              <template #overlay>
                <a-menu @click="({ key }: any) => onAudit(record, key)">
                  <a-menu-item key="1">通过（已发布）</a-menu-item>
                  <a-menu-item key="0">驳回（待审核）</a-menu-item>
                  <a-menu-item key="2" danger>下架</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-popconfirm title="确认删除这套搭配？" ok-text="删除" @confirm="onRemove(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 编辑抽屉 -->
    <a-drawer
      v-model:open="editor.open"
      :title="editor.mode === 'add' ? '新增搭配' : '编辑搭配'"
      width="600px"
      :body-style="{ paddingBottom: '68px' }"
    >
      <a-form layout="vertical">
        <div class="form-grid">
          <a-form-item label="标题" required>
            <a-input v-model:value="editor.form.title" :maxlength="50" />
          </a-form-item>
          <a-form-item label="副标题">
            <a-input v-model:value="editor.form.subtitle" :maxlength="60" />
          </a-form-item>
          <a-form-item label="风格">
            <a-select v-model:value="editor.form.styleTag" allow-clear>
              <a-select-option v-for="d in builtinDicts.fashion_style" :key="d.value" :value="d.value">{{ d.text }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="场景">
            <a-select v-model:value="editor.form.sceneTag" allow-clear>
              <a-select-option v-for="s in sceneOptions" :key="s.value" :value="s.value">{{ s.text }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="色调">
            <a-select v-model:value="editor.form.colorTone" allow-clear>
              <a-select-option v-for="d in builtinDicts.fashion_color" :key="d.value" :value="d.value">{{ d.text }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="editor.form.status">
              <a-select-option v-for="o in outfitStatusOptions" :key="o.value" :value="o.value">{{ o.text }}</a-select-option>
            </a-select>
          </a-form-item>
        </div>

        <a-form-item label="封面图">
          <div class="upload-row">
            <a-upload :show-upload-list="false" accept="image/jpeg,image/png,image/webp" :custom-request="doUpload">
              <a-button><upload-outlined /> 上传封面</a-button>
            </a-upload>
            <span class="tiny muted">可留空，默认取首件时装图</span>
          </div>
          <a-input v-model:value="editor.form.coverImage" class="upload-url" placeholder="/images/outfit/outfit-01.jpg" />
          <img v-if="editor.form.coverImage" :src="imgUrl(editor.form.coverImage)" class="upload-preview upload-preview--land" />
        </a-form-item>

        <a-form-item label="描述">
          <a-textarea v-model:value="editor.form.description" :rows="3" :maxlength="1000" />
        </a-form-item>

        <a-form-item required label="包含时装（按展示顺序，上限 6 件）">
          <a-select
            v-model:value="editor.itemIds"
            mode="multiple"
            placeholder="搜索并选择时装"
            option-filter-prop="children"
            :max-tag-count="6"
          >
            <a-select-option v-for="(it, i) in itemOptions" :key="it.id" :value="it.id">
              【{{ i + 1 }}】{{ it.name }}
            </a-select-option>
          </a-select>
          <div v-if="editor.itemIds.length" class="picked-list">
            <div v-for="id in editor.itemIds" :key="id" class="picked-row">
              <img :src="imgUrl(itemById(id)?.imageUrl)" class="picked-thumb" />
              <span class="grow tiny">{{ itemById(id)?.name }}</span>
              <span class="mono">{{ itemById(id)?.code }}</span>
            </div>
          </div>
        </a-form-item>
      </a-form>

      <div class="drawer-foot">
        <a-button style="margin-right: 8px" @click="editor.open = false">取消</a-button>
        <a-button type="primary" :loading="editor.saving" @click="save">保存</a-button>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import {
  itemApi,
  outfitApi,
  type FashionItem,
  type OutfitRow,
  type PageData,
} from '../../api'
import { builtinDicts, dictText, outfitStatusMeta, outfitStatusOptions } from '../../utils/dict'
import { errText, formatNumber, imgUrl } from '../../utils/format'
import { apiUpload } from '../../api/client'

const sceneOptions = [
  { text: '日常', value: 'daily' },
  { text: '战阵', value: 'battle' },
  { text: '集会', value: 'festival' },
  { text: '闲居', value: 'home' },
  { text: '出行', value: 'travel' },
]

const loading = ref(false)
const rows = ref<OutfitRow[]>([])
const selectedRowKeys = ref<string[]>([])
const filters = reactive<Record<string, string>>({ title: '', status: '', styleTag: '' })
const itemOptions = ref<FashionItem[]>([])

const columns = [
  { title: '封面', key: 'cover', width: 92 },
  { title: '标题', key: 'title', width: 260 },
  { title: '作者', key: 'author', width: 100 },
  { title: '风格', key: 'style', width: 84 },
  { title: '件数', key: 'count', width: 64 },
  { title: '互动', key: 'stats', width: 130 },
  { title: '状态', key: 'status', width: 92 },
  { title: '操作', key: 'actions', width: 170 },
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
    if (filters.title) q.title = filters.title
    if (filters.status !== '') q.status = filters.status
    if (filters.styleTag) q.styleTag = filters.styleTag
    const data: PageData<OutfitRow> = await outfitApi.list(q)
    rows.value = data.records || []
    pagination.total = data.total || 0
  } catch (e) {
    message.error(errText(e))
  } finally {
    loading.value = false
  }
}

function reload() {
  pagination.current = 1
  selectedRowKeys.value = []
  load()
}

function onTableChange(p: { current: number; pageSize: number }) {
  pagination.current = p.current
  pagination.pageSize = p.pageSize
  load()
}

function resetFilters() {
  Object.assign(filters, { title: '', status: '', styleTag: '' })
  reload()
}

/* ---------- 审核 / 删除 ---------- */
async function onAudit(row: OutfitRow, status: string) {
  try {
    await outfitApi.audit(row.id, Number(status))
    message.success('审核状态已更新')
    load()
  } catch (e) {
    message.error(errText(e))
  }
}

function onRemove(row: OutfitRow) {
  outfitApi
    .remove(row.id)
    .then(() => {
      message.success('已删除')
      load()
    })
    .catch((e) => message.error(errText(e)))
}

function onRemoveBatch() {
  outfitApi
    .removeBatch(selectedRowKeys.value)
    .then(() => {
      message.success('已批量删除')
      selectedRowKeys.value = []
      load()
    })
    .catch((e) => message.error(errText(e)))
}

/* ---------- 编辑器 ---------- */
const emptyForm = () => ({
  id: '',
  title: '',
  subtitle: '',
  description: '',
  coverImage: '',
  styleTag: 'wuxia',
  sceneTag: 'daily',
  colorTone: 'ink',
  status: 1,
})
const editor = reactive({
  open: false,
  saving: false,
  mode: 'add' as 'add' | 'edit',
  form: emptyForm() as Record<string, any>,
  itemIds: [] as string[],
})

const itemById = (id: string) => itemOptions.value.find((i) => i.id === id)

function openEditor(row?: OutfitRow) {
  editor.mode = row ? 'edit' : 'add'
  editor.form = row ? { ...emptyForm(), ...row } : emptyForm()
  editor.itemIds = []
  if (row) {
    // 拉详情拿 items 顺序
    outfitApi
      .detail(row.id)
      .then((d) => {
        editor.itemIds = (d.items || []).map((i) => i.id)
      })
      .catch((e) => message.error(errText(e)))
  }
  editor.open = true
}

async function doUpload(opt: { file: File; onSuccess?: (r: unknown) => void; onError?: (e: unknown) => void }) {
  try {
    const res = await apiUpload(opt.file)
    editor.form.coverImage = res.url
    message.success('上传成功')
    opt.onSuccess?.(res)
  } catch (e) {
    message.error(errText(e, '上传失败'))
    opt.onError?.(e)
  }
}

async function save() {
  if (!editor.form.title) {
    message.warning('标题必填')
    return
  }
  if (!editor.itemIds.length) {
    message.warning('至少选择 1 件时装')
    return
  }
  editor.saving = true
  const body: Record<string, unknown> = { ...editor.form, itemIds: editor.itemIds }
  try {
    if (editor.mode === 'add') {
      delete body.id
      await outfitApi.add(body)
      message.success('新增成功')
    } else {
      await outfitApi.edit(body)
      message.success('保存成功')
    }
    editor.open = false
    load()
  } catch (e) {
    message.error(errText(e))
  } finally {
    editor.saving = false
  }
}

onMounted(() => {
  load()
  itemApi.list({ pageNo: 1, pageSize: 200 }).then((d) => {
    itemOptions.value = d.records || []
  })
})
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
  display: block;
  font-size: 13px;
}

.tag.tag--soft {
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  font-size: 12px;
  background: var(--color-paper-200);
  color: var(--text-secondary);
}

.status-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 14px;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.upload-url {
  margin-top: 10px;
}

.upload-preview {
  display: block;
  width: 140px;
  height: 190px;
  object-fit: cover;
  border-radius: var(--radius-6);
  border: 1px solid var(--border);
  margin-top: 10px;
}

.upload-preview--land {
  width: 220px;
  height: 130px;
}

.picked-list {
  margin-top: 10px;
  border: 1px dashed var(--border);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.picked-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.picked-thumb {
  width: 34px;
  height: 46px;
  object-fit: cover;
  border-radius: 4px;
}

.drawer-foot {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 10px 20px;
  background: var(--surface-raised);
  border-top: 1px solid var(--divider);
  text-align: right;
}
</style>
