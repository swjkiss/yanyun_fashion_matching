<template>
  <div class="page items">
    <div class="page-head">
      <div>
        <h2 class="page-title">时装图鉴</h2>
        <p class="page-sub">全量时装资产维护：新增、编辑、上下架与检索。</p>
      </div>
      <div class="row">
        <a-button @click="openRules">
          <template #icon><info-circle-outlined /></template>
          编码规则
        </a-button>
        <a-button type="primary" @click="openEditor()">
          <template #icon><plus-outlined /></template>
          新增时装
        </a-button>
      </div>
    </div>

    <!-- 过滤区 -->
    <div class="panel panel--filter">
      <div class="filter-grid">
        <a-input v-model:value="filters.name" placeholder="名称 / 编码" allow-clear class="filter-item" @press-enter="reload" />
        <a-select v-model:value="filters.category" placeholder="部位" allow-clear class="filter-item">
          <a-select-option v-for="d in builtinDicts.fashion_category" :key="d.value" :value="d.value">{{ d.text }}</a-select-option>
        </a-select>
        <a-select v-model:value="filters.rarity" placeholder="稀有度" allow-clear class="filter-item">
          <a-select-option v-for="d in builtinDicts.fashion_rarity" :key="d.value" :value="d.value">{{ d.text }}</a-select-option>
        </a-select>
        <a-select v-model:value="filters.colorFamily" placeholder="色系" allow-clear class="filter-item">
          <a-select-option v-for="d in builtinDicts.fashion_color" :key="d.value" :value="d.value">{{ d.text }}</a-select-option>
        </a-select>
        <a-select v-model:value="filters.status" placeholder="状态" allow-clear class="filter-item">
          <a-select-option :value="1">上架</a-select-option>
          <a-select-option :value="0">下架</a-select-option>
        </a-select>
        <div class="filter-actions">
          <a-button type="primary" @click="reload">查询</a-button>
          <a-button @click="resetFilters">重置</a-button>
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-bar">
        <span class="panel-bar-total num">共 {{ page.total }} 件</span>
        <a-popconfirm
          title="确认删除选中的时装？"
          ok-text="删除"
          :disabled="!selectedRowKeys.length"
          @confirm="onRemoveBatch"
        >
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
            <a-image :src="imgUrl(record.imageUrl)" :width="46" :height="62" style="object-fit: cover; border-radius: 6px" fallback="" />
          </template>
          <template v-else-if="column.key === 'name'">
            <div class="cell-name">
              <b>{{ record.name }}</b>
              <span class="mono">{{ record.code }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'category'">
            <span class="tag tag--soft">{{ dictText('fashion_category', record.category) }}</span>
          </template>
          <template v-else-if="column.key === 'rarity'">
            <span class="rarity-tag" :class="rarityClass(record.rarity)">{{ dictText('fashion_rarity', record.rarity) }}</span>
          </template>
          <template v-else-if="column.key === 'color'">
            <span class="row-tight">
              <i class="color-dot" :style="{ background: colorFamilyColor(record.colorFamily) }" />
              {{ dictText('fashion_color', record.colorFamily) }}
            </span>
          </template>
          <template v-else-if="column.key === 'stats'">
            <span class="tiny muted">看 {{ formatNumber(record.viewCount) }} · 赞 {{ formatNumber(record.likeCount) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 1"
              checked-children="上"
              un-checked-children="下"
              @change="(v: any) => onToggleStatus(record, v as number)"
            />
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="openEditor(record)">编辑</a-button>
            <a-popconfirm title="确认删除这件时装？" ok-text="删除" @confirm="onRemove(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新增 / 编辑抽屉 -->
    <a-drawer
      v-model:open="editor.open"
      :title="editor.mode === 'add' ? '新增时装' : '编辑时装'"
      width="560px"
      :body-style="{ paddingBottom: '68px' }"
    >
      <a-form layout="vertical" @finish="save">
        <div class="form-grid">
          <a-form-item label="名称" required>
            <a-input v-model:value="editor.form.name" placeholder="如 玄刃·墨鳞甲" :maxlength="50" />
          </a-form-item>
          <a-form-item label="编码" required>
            <a-input v-model:value="editor.form.code" placeholder="YY-FW-001" :maxlength="20" />
          </a-form-item>
          <a-form-item label="部位">
            <a-select v-model:value="editor.form.category">
              <a-select-option v-for="d in builtinDicts.fashion_category" :key="d.value" :value="d.value">{{ d.text }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="稀有度">
            <a-select v-model:value="editor.form.rarity">
              <a-select-option v-for="d in builtinDicts.fashion_rarity" :key="d.value" :value="d.value">{{ d.text }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="色系">
            <a-select v-model:value="editor.form.colorFamily" allow-clear>
              <a-select-option v-for="d in builtinDicts.fashion_color" :key="d.value" :value="d.value">{{ d.text }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="风格">
            <a-select v-model:value="editor.form.style" allow-clear>
              <a-select-option v-for="d in builtinDicts.fashion_style" :key="d.value" :value="d.value">{{ d.text }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="性别">
            <a-radio-group v-model:value="editor.form.gender">
              <a-radio v-for="g in genderOptions" :key="g.value" :value="g.value">{{ g.text }}</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="系列">
            <a-input v-model:value="editor.form.series" placeholder="如 玄刃" :maxlength="32" />
          </a-form-item>
          <a-form-item label="排重要序" class="form-grid__num">
            <a-input-number v-model:value="editor.form.sort" :min="0" :max="999" style="width: 100%" />
          </a-form-item>
        </div>

        <a-form-item label="封面图">
          <div class="upload-row">
            <a-upload
              :show-upload-list="false"
              accept="image/jpeg,image/png,image/webp"
              :custom-request="doUpload"
            >
              <a-button><upload-outlined /> 上传图片</a-button>
            </a-upload>
            <span class="tiny muted">JPG / PNG / WebP ≤ 5MB，落盘 /images/uploads/</span>
          </div>
          <a-input v-model:value="editor.form.imageUrl" class="upload-url" placeholder="/images/fashion/fashion-01.jpg" />
          <img v-if="editor.form.imageUrl" :src="imgUrl(editor.form.imageUrl)" class="upload-preview" />
        </a-form-item>

        <a-form-item label="一句话卖点" :maxlength="120">
          <a-input v-model:value="editor.form.summary" :maxlength="120" show-count />
        </a-form-item>
        <a-form-item label="详情描述">
          <a-textarea v-model:value="editor.form.description" :rows="3" :maxlength="1000" />
        </a-form-item>
        <a-form-item label="背景故事">
          <a-textarea v-model:value="editor.form.story" :rows="3" :maxlength="1000" />
        </a-form-item>
        <a-form-item label="标签（逗号分隔）">
          <a-input v-model:value="editor.form.tags" placeholder="铠甲,重装,北境" />
        </a-form-item>
        <div class="form-grid form-grid--last">
          <a-form-item label="获取途径">
            <a-input v-model:value="editor.form.obtainMethod" :maxlength="100" />
          </a-form-item>
          <a-form-item label="上线日期">
            <a-date-picker v-model:value="releaseDate" style="width: 100%" value-format="YYYY-MM-DD" />
          </a-form-item>
        </div>
        <a-form-item label="获取说明">
          <a-textarea v-model:value="editor.form.obtainDetail" :rows="2" :maxlength="255" />
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
import { createVNode, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { ExclamationCircleOutlined, InfoCircleOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { itemApi, type FashionItem, type PageData } from '../../api'
import {
  builtinDicts,
  colorFamilyColor,
  dictText,
  genderOptions,
  rarityClass,
} from '../../utils/dict'
import { errText, formatNumber, imgUrl } from '../../utils/format'
import { apiUpload } from '../../api/client'

const loading = ref(false)
const rows = ref<FashionItem[]>([])
const page = reactive({ total: 0, current: 1, size: 10 })
const selectedRowKeys = ref<string[]>([])
const filters = reactive<Record<string, string>>({ name: '', category: '', rarity: '', colorFamily: '', status: '' })

const columns = [
  { title: '封面', key: 'cover', width: 76 },
  { title: '名称', key: 'name', width: 210 },
  { title: '部位', key: 'category', width: 76 },
  { title: '稀有度', key: 'rarity', width: 76 },
  { title: '色系', key: 'color', width: 92 },
  { title: '热度', key: 'stats', width: 120 },
  { title: '排序', dataIndex: 'sort', width: 64 },
  { title: '上下架', key: 'status', width: 84 },
  { title: '操作', key: 'actions', width: 120 },
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
    if (filters.name) q.name = filters.name
    if (filters.category) q.category = filters.category
    if (filters.rarity) q.rarity = filters.rarity
    if (filters.colorFamily) q.colorFamily = filters.colorFamily
    if (filters.status !== '') q.status = filters.status
    const data: PageData<FashionItem> = await itemApi.list(q)
    rows.value = data.records || []
    pagination.total = data.total || 0
    page.total = data.total || 0
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
  Object.assign(filters, { name: '', category: '', rarity: '', colorFamily: '', status: '' })
  reload()
}

/* ---------- 状态 / 删除 ---------- */
async function onToggleStatus(row: FashionItem, status: number) {
  try {
    await itemApi.status(row.id, status)
    message.success(status === 1 ? '已上架' : '已下架')
    load()
  } catch (e) {
    message.error(errText(e))
    load()
  }
}

function onRemove(row: FashionItem) {
  itemApi
    .remove(row.id)
    .then(() => {
      message.success('已删除')
      load()
    })
    .catch((e) => message.error(errText(e)))
}

function onRemoveBatch() {
  Modal.confirm({
    title: '批量删除',
    icon: createVNode(ExclamationCircleOutlined),
    content: `将删除选中 ${selectedRowKeys.value.length} 件时装，操作不可恢复。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await itemApi.removeBatch(selectedRowKeys.value)
        message.success('已批量删除')
        selectedRowKeys.value = []
        load()
      } catch (e) {
        message.error(errText(e))
      }
    },
  })
}

/* ---------- 编辑器 ---------- */
const emptyForm = () => ({
  id: '',
  name: '',
  code: '',
  glyph: '',
  category: 'set',
  rarity: 'common',
  colorFamily: 'ink',
  style: 'wuxia',
  series: '',
  gender: 'unisex',
  imageUrl: '',
  summary: '',
  description: '',
  story: '',
  tags: '',
  obtainMethod: '',
  obtainDetail: '',
  releaseDate: '',
  status: 1,
  sort: 50,
  isRecommend: 0,
})
const editor = reactive({
  open: false,
  saving: false,
  mode: 'add' as 'add' | 'edit',
  form: emptyForm() as Record<string, any>,
})

/** a-date-picker 的受控镜像 */
const releaseDate = ref('')

function openEditor(row?: FashionItem) {
  editor.mode = row ? 'edit' : 'add'
  editor.form = row ? { ...emptyForm(), ...row } : emptyForm()
  releaseDate.value = (row?.releaseDate as string) || ''
  editor.open = true
}

async function doUpload(opt: { file: File; onSuccess?: (r: unknown) => void; onError?: (e: unknown) => void }) {
  try {
    const res = await apiUpload(opt.file)
    editor.form.imageUrl = res.url
    message.success('上传成功')
    opt.onSuccess?.(res)
  } catch (e) {
    message.error(errText(e, '上传失败'))
    opt.onError?.(e)
  }
}

async function save() {
  if (!editor.form.name || !editor.form.code) {
    message.warning('名称与编码为必填项')
    return
  }
  editor.saving = true
  const body: Record<string, unknown> = { ...editor.form, releaseDate: releaseDate.value || (editor.form.releaseDate as string) }
  if (!body.releaseDate) delete body.releaseDate
  try {
    if (editor.mode === 'add') {
      delete body.id
      await itemApi.add(body)
      message.success('新增成功')
    } else {
      await itemApi.edit(body)
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

function openRules() {
  Modal.info({
    title: '时装编码规则',
    content: '编码用于唯一识别资产，推荐 YY-FW-###（FW = Fashion Wardrobe）。部位/稀有度/色系/风格取值均来自数据字典，新增取值请与产品确认。',
    okText: '知道了',
  })
}

onMounted(load)
</script>

<style scoped>
.panel {
  background: var(--surface-raised);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px 18px;
  margin-bottom: var(--space-4);
}

.panel--filter {
  margin-bottom: var(--space-4);
}

.filter-grid {
  display: grid;
  grid-template-columns: 1.4fr repeat(4, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.filter-item,
.filter-grid :deep(.ant-select) {
  width: 100%;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.panel-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-bar-total {
  font-size: 13px;
  color: var(--text-tertiary);
}

.cell-name b {
  display: block;
  font-size: 13px;
  color: var(--text-primary);
}

.tag.tag--soft {
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  font-size: 12px;
  background: var(--color-paper-200);
  color: var(--text-secondary);
}

.color-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 5px;
  vertical-align: -1px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 14px;
}

.form-grid--last {
  margin-top: 0;
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
