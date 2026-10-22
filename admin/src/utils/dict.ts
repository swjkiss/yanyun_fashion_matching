/**
 * 字典与视觉映射。
 *
 * 数据事实来源：`docs/contract-data.md` 第 1 节。
 * 后端 `/sys/dict/getDictItems/{dictCode}` 可用时由 app store 覆盖文案；
 * 这里保留一份内置字典，保证后端未启动 / 字典接口异常时后台依然可用。
 * 颜色一律返回 CSS 变量引用（见 `styles/tokens.css`），不写死色值。
 */

export type DictCode =
  | 'fashion_category'
  | 'fashion_rarity'
  | 'fashion_color'
  | 'fashion_style'

export interface DictItem {
  text: string
  value: string
  title?: string
  /** 稀有度 / 色系主色（后端字典可能返回） */
  color?: string
}

export type DictSet = Record<DictCode, DictItem[]>

export const builtinDicts: DictSet = {
  fashion_category: [
    { text: '套装', value: 'set' },
    { text: '上衣', value: 'top' },
    { text: '下装', value: 'bottom' },
    { text: '头饰', value: 'headwear' },
    { text: '配饰', value: 'accessory' },
    { text: '发型', value: 'hairstyle' },
  ],
  fashion_rarity: [
    { text: '凡品', value: 'common' },
    { text: '良品', value: 'fine' },
    { text: '珍品', value: 'rare' },
    { text: '绝品', value: 'epic' },
    { text: '传说', value: 'legend' },
  ],
  fashion_color: [
    { text: '玄墨', value: 'ink' },
    { text: '朱砂', value: 'cinnabar' },
    { text: '鎏金', value: 'gold' },
    { text: '月白', value: 'moon' },
    { text: '竹青', value: 'bamboo' },
    { text: '靛青', value: 'indigo' },
    { text: '绛紫', value: 'purple' },
    { text: '赭石', value: 'ochre' },
  ],
  fashion_style: [
    { text: '侠客', value: 'wuxia' },
    { text: '庙堂', value: 'court' },
    { text: '文人', value: 'scholar' },
    { text: '江湖', value: 'jianghu' },
    { text: '仙侠', value: 'xianxia' },
    { text: '异域', value: 'exotic' },
  ],
}

/** 稀有度 → 视觉变量（文字 / 底 / 描边） */
export const rarityVars: Record<string, { color: string; bg: string; border: string }> = {
  common: {
    color: 'var(--rarity-common)',
    bg: 'var(--rarity-common-bg)',
    border: 'var(--rarity-common-border)',
  },
  fine: {
    color: 'var(--rarity-fine)',
    bg: 'var(--rarity-fine-bg)',
    border: 'var(--rarity-fine-border)',
  },
  rare: {
    color: 'var(--rarity-rare)',
    bg: 'var(--rarity-rare-bg)',
    border: 'var(--rarity-rare-border)',
  },
  epic: {
    color: 'var(--rarity-epic)',
    bg: 'var(--rarity-epic-bg)',
    border: 'var(--rarity-epic-border)',
  },
  legend: {
    color: 'var(--rarity-legend)',
    bg: 'var(--rarity-legend-bg)',
    border: 'var(--rarity-legend-border)',
  },
}

/** 八色系 → 主色 / 浅底 / 深色（tokens.css 的 palette 变量） */
export const colorFamilyVars: Record<string, { main: string; soft: string; deep: string }> = {
  ink: {
    main: 'var(--color-ink-800)',
    soft: 'var(--color-ink-100)',
    deep: 'var(--color-ink-900)',
  },
  cinnabar: {
    main: 'var(--color-cinnabar-500)',
    soft: 'var(--color-cinnabar-100)',
    deep: 'var(--color-cinnabar-600)',
  },
  gold: {
    main: 'var(--color-gold-500)',
    soft: 'var(--color-gold-100)',
    deep: 'var(--color-gold-600)',
  },
  moon: {
    main: 'var(--color-moon-500)',
    soft: 'var(--color-moon-100)',
    deep: 'var(--color-moon-600)',
  },
  bamboo: {
    main: 'var(--color-bamboo-500)',
    soft: 'var(--color-bamboo-100)',
    deep: 'var(--color-bamboo-600)',
  },
  indigo: {
    main: 'var(--color-indigo-500)',
    soft: 'var(--color-indigo-100)',
    deep: 'var(--color-indigo-600)',
  },
  purple: {
    main: 'var(--color-purple-500)',
    soft: 'var(--color-purple-100)',
    deep: 'var(--color-purple-600)',
  },
  ochre: {
    main: 'var(--color-ochre-500)',
    soft: 'var(--color-ochre-100)',
    deep: 'var(--color-ochre-600)',
  },
}

/** 稀有度标签 class 后缀（配合 global.css 的 .rarity-tag.is-*） */
export function rarityClass(value?: string): string {
  return value && rarityVars[value] ? `is-${value}` : ''
}

/** 稀有度主色（图表用） */
export function rarityColor(value?: string): string {
  return (value && rarityVars[value] ? rarityVars[value] : rarityVars.common).color
}

/** 色系主色（图表 / 色点用） */
export function colorFamilyColor(value?: string): string {
  return value && colorFamilyVars[value]
    ? colorFamilyVars[value].main
    : 'var(--color-ink-200)'
}

/** 色系浅底 */
export function colorFamilySoft(value?: string): string {
  return value && colorFamilyVars[value]
    ? colorFamilyVars[value].soft
    : 'var(--color-paper-200)'
}

/** 按 value 找字典项 */
export function dictItem(code: DictCode, value?: string | null): DictItem | undefined {
  if (!value) return undefined
  return builtinDicts[code].find((item) => item.value === value)
}

/** 按 value 取文案，找不到回退原值 */
export function dictText(code: DictCode, value?: string | null): string {
  if (!value) return '-'
  return dictItem(code, value)?.text ?? value
}

/** 按文案反查 value（统计接口只回中文名） */
export function dictValueByText(code: DictCode, text?: string | null): string | undefined {
  if (!text) return undefined
  return builtinDicts[code].find((item) => item.text === text)?.value
}

/** 稀有度中文名 → 主色 */
export function rarityColorByText(text?: string | null): string {
  const value = dictValueByText('fashion_rarity', text)
  return rarityColor(value)
}

/** 色系中文名 → 主色 */
export function colorColorByText(text?: string | null): string {
  const value = dictValueByText('fashion_color', text)
  return colorFamilyColor(value)
}

/** 时装状态 */
export const itemStatusOptions = [
  { text: '上架', value: 1 },
  { text: '下架', value: 0 },
]

/** 搭配状态（1 已发布 / 0 待审核 / 2 已下架） */
export const outfitStatusOptions = [
  { text: '已发布', value: 1 },
  { text: '待审核', value: 0 },
  { text: '已下架', value: 2 },
]

export function outfitStatusMeta(status?: number | null): { text: string; color: string; bg: string } {
  switch (status) {
    case 1:
      return { text: '已发布', color: 'var(--success)', bg: 'var(--color-jade-100)' }
    case 0:
      return { text: '待审核', color: 'var(--warning)', bg: 'var(--color-ochre-100)' }
    case 2:
      return { text: '已下架', color: 'var(--text-tertiary)', bg: 'var(--color-paper-200)' }
    default:
      return { text: '未知', color: 'var(--text-placeholder)', bg: 'var(--color-paper-200)' }
  }
}

export const genderOptions = [
  { text: '女', value: 'female' },
  { text: '男', value: 'male' },
  { text: '通用', value: 'unisex' },
]
