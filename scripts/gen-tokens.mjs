#!/usr/bin/env node
/**
 * 设计令牌编译器：design/tokens.json → 各端可用格式。
 *
 * 生成物（勿手改，改 tokens.json 后执行 `pnpm tokens`）：
 *   admin/src/styles/tokens.css     :root { --color-ink-900: #100E0C; ... }
 *   miniapp/src/styles/tokens.scss  $ink-900: #100E0C; ... （长度单位 ×2 → rpx）
 *
 * 之所以由脚本生成而不是纯手写：两端必须严格同源，否则"统一设计语言"无从保证。
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const tokens = JSON.parse(readFileSync(resolve(ROOT, 'design/tokens.json'), 'utf8'))

const kebab = (s) => s.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase()
const px = (n) => `${n}px`
const rpx = (n) => `${n * 2}rpx` // 750 设计稿基准

const header = (target) =>
  `/* 由 scripts/gen-tokens.mjs 从 design/tokens.json 生成 —— 请勿手动修改 */\n` +
  `/* target: ${target} | generated: ${new Date().toISOString()} */\n\n`

// ---------------------------------------------------------------- admin (CSS)
function buildCss() {
  const L = []
  const push = (sel, decls) => L.push(`${sel} {\n${decls.map((d) => `  ${d}`).join('\n')}\n}\n`)

  const palette = []
  for (const [group, steps] of Object.entries(tokens.color)) {
    for (const [step, value] of Object.entries(steps)) {
      palette.push(`--color-${group}-${step}: ${value};`)
    }
  }
  const semantic = Object.entries(tokens.semantic).map(([k, v]) => `--${kebab(k)}: ${v};`)
  const rarityFlat = []
  for (const [k, v] of Object.entries(tokens.rarity)) {
    rarityFlat.push(`--rarity-${k}: ${v.color};`, `--rarity-${k}-bg: ${v.bg};`, `--rarity-${k}-border: ${v.border};`)
  }
  const space = Object.entries(tokens.space).map(([k, v]) => `--space-${k}: ${px(v)};`)
  const radius = Object.entries(tokens.radius).map(([k, v]) => `--radius-${k}: ${px(v)};`)
  const shadow = Object.entries(tokens.shadow).map(([k, v]) => `--shadow-${k}: ${v};`)
  const motion = Object.entries(tokens.motion).map(([k, v]) => `--motion-${k}: ${v};`)
  const fonts = Object.entries(tokens.font).map(([k, v]) => `--font-${k}: ${v};`)
  const sizes = Object.entries(tokens.fontSize).map(([k, v]) => `--text-${k}: ${px(v)};`)
  const weights = Object.entries(tokens.fontWeight).map(([k, v]) => `--weight-${k}: ${v};`)
  const leading = Object.entries(tokens.lineHeight).map(([k, v]) => `--leading-${k}: ${v};`)
  const tracking = Object.entries(tokens.letterSpacing).map(([k, v]) => `--tracking-${k}: ${v};`)
  const z = Object.entries(tokens.zIndex).map(([k, v]) => `--z-${k}: ${v};`)
  const layout = Object.entries(tokens.layout).map(([k, v]) => `--layout-${kebab(k)}: ${v};`)

  push(':root', [
    '/* palette */', ...palette, '',
    '/* semantic */', ...semantic, '',
    '/* rarity */', ...rarityFlat, '',
    '/* spacing */', ...space, '',
    '/* radius */', ...radius, '',
    '/* shadow */', ...shadow, '',
    '/* motion */', ...motion, '',
    '/* typography */', ...fonts, ...sizes, ...weights, ...leading, ...tracking, '',
    '/* layers */', ...z, '',
    '/* layout */', ...layout,
  ])
  return header('admin CSS variables') + L.join('\n')
}

// -------------------------------------------------------------- miniapp (SCSS)
function buildScss() {
  const L = []
  for (const [group, steps] of Object.entries(tokens.color)) {
    for (const [step, value] of Object.entries(steps)) L.push(`$${group}-${step}: ${value};`)
  }
  L.push('')
  for (const [k, v] of Object.entries(tokens.semantic)) L.push(`$${kebab(k)}: ${v};`)
  L.push('')
  for (const [k, v] of Object.entries(tokens.rarity)) {
    L.push(`$rarity-${k}: ${v.color};`, `$rarity-${k}-bg: ${v.bg};`, `$rarity-${k}-border: ${v.border};`)
  }
  L.push('', '// 间距 / 圆角 / 字号在 750 设计稿下按 2 倍换算为 rpx', '')
  for (const [k, v] of Object.entries(tokens.space)) L.push(`$space-${k}: ${rpx(v)};`)
  L.push('')
  for (const [k, v] of Object.entries(tokens.radius)) L.push(`$radius-${k}: ${rpx(v)};`)
  L.push('')
  for (const [k, v] of Object.entries(tokens.fontSize)) L.push(`$text-${k}: ${rpx(v)};`)
  L.push('')
  for (const [k, v] of Object.entries(tokens.fontWeight)) L.push(`$weight-${k}: ${v};`)
  L.push('')
  for (const [k, v] of Object.entries(tokens.lineHeight)) L.push(`$leading-${k}: ${v};`)
  L.push('')
  for (const [k, v] of Object.entries(tokens.letterSpacing)) L.push(`$tracking-${k}: ${v};`)
  L.push('')
  for (const [k, v] of Object.entries(tokens.motion)) L.push(`$motion-${k}: ${v};`)
  L.push('')
  for (const [k, v] of Object.entries(tokens.shadow)) {
    // 阴影模糊/偏移同步放大，保证 rpx 尺度下观感一致
    L.push(`$shadow-${k}: ${v.replace(/(-?\d+(?:\.\d+)?)px/g, (_, n) => `${Math.round(parseFloat(n) * 2)}rpx`)};`)
  }
  L.push('')
  L.push('// 字体族：小程序不支持外部字体，使用系统衬线族保证古风观感', '')
  for (const [k, v] of Object.entries(tokens.font)) L.push(`$font-${k}: ${v};`)

  // --- 数值别名层：页面侧以 rpx 数值直呼（$text-28 / $space-32 / $radius-12）---
  // 与上面 slug 形式的变量并存；仅覆盖与本工程习惯一致的数值命名。
  L.push('')
  L.push('// 数值别名（rpx 直呼，供页面便捷使用）', '')
  for (let n = 18; n <= 76; n += 2) L.push(`$text-${n}: ${n}rpx;`)
  L.push('')
  for (const n of [4, 8, 12, 16, 20, 24, 28, 32, 40, 48, 56, 64, 72, 96]) {
    L.push(`$space-${n}: ${n}rpx;`)
  }
  L.push('')
  for (const n of [4, 6, 8, 10, 12, 16, 999]) L.push(`$radius-${n}: ${n === 999 ? 999 : n * 2}rpx;`)

  return header('miniapp SCSS variables') + L.join('\n') + '\n'
}

function emit(relPath, content) {
  const target = resolve(ROOT, relPath)
  mkdirSync(dirname(target), { recursive: true })
  writeFileSync(target, content, 'utf8')
  console.log(`  ✓ ${relPath}  (${content.length} B)`)
}

console.log('编译设计令牌 …')
emit('admin/src/styles/tokens.css', buildCss())
emit('miniapp/src/styles/tokens.scss', buildScss())
console.log('完成。')
