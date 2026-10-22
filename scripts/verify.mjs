#!/usr/bin/env node
/**
 * 交付前自检：把"能不能跑"变成可执行断言，而不是靠人肉点页面。
 *
 * 覆盖五类：
 *   1. 静态产物（令牌、图片、三端 dist、后端 jar）
 *   2. 数据契约一致性（文档里的 16 件时装 / 8 套搭配 是否真的落到 data.sql）
 *   3. 数据库（MySQL 可连、表行数）
 *   4. 接口（服务端若在跑，逐个打真实请求校验 envelope 与业务值）
 *   5. 设计令牌新鲜度（tokens.json 改过但没重新生成 → 报警）
 *
 * 用法：node scripts/verify.mjs
 */
import { spawnSync } from 'node:child_process'
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { API, DB, MYSQL, PATHS, PORTS, ROOT } from './env.mjs'

const C = { dim: '\x1b[2m', green: '\x1b[32m', red: '\x1b[31m', yellow: '\x1b[33m', bold: '\x1b[1m', reset: '\x1b[0m' }

const results = []
const check = (group, name, fn) => results.push({ group, name, fn })
const pass = (msg) => ({ ok: true, msg })
const fail = (msg) => ({ ok: false, msg })

// ------------------------------------------------------------------ 1. 产物
check('产物', '设计令牌文件存在', () => {
  const missing = [PATHS.adminTokens, PATHS.miniTokens].filter((p) => !existsSync(p))
  return missing.length ? fail(`缺少 ${missing.join(', ')}`) : pass('tokens.css / tokens.scss 已生成')
})

check('产物', '设计令牌是最新的', () => {
  if (!existsSync(PATHS.adminTokens)) return fail('tokens.css 不存在')
  const src = statSync(PATHS.tokensJson).mtimeMs
  const out = statSync(PATHS.adminTokens).mtimeMs
  return out >= src ? pass('生成物晚于 tokens.json') : fail('tokens.json 已修改，请执行 pnpm tokens')
})

check('产物', '时装图片 16 张', () => {
  const dir = join(PATHS.images, 'fashion')
  if (!existsSync(dir)) return fail('backend/upload/images/fashion 不存在')
  const files = readdirSync(dir).filter((f) => /^fashion-\d+\.jpg$/i.test(f))
  return files.length >= 16 ? pass(`${files.length} 张`) : fail(`只有 ${files.length} 张，期望 16`)
})

check('产物', '搭配封面 ≥6 张', () => {
  const dir = join(PATHS.images, 'outfit')
  if (!existsSync(dir)) return fail('backend/upload/images/outfit 不存在')
  const files = readdirSync(dir).filter((f) => /^outfit-\d+\.jpg$/i.test(f))
  return files.length >= 6 ? pass(`${files.length} 张`) : fail(`只有 ${files.length} 张`)
})

check('产物', '主视觉与头像', () => {
  const hero = join(PATHS.images, 'hero-banner.jpg')
  const avatars = existsSync(join(PATHS.images, 'avatar'))
    ? readdirSync(join(PATHS.images, 'avatar')).filter((f) => /^avatar-\d+\.jpg$/i.test(f)).length
    : 0
  const okHero = existsSync(hero)
  return okHero && avatars >= 3 ? pass(`hero 存在，头像 ${avatars} 张`) : fail(`hero=${okHero}, 头像=${avatars}`)
})

check('产物', '后台构建产物', () =>
  existsSync(join(PATHS.adminDist, 'index.html')) ? pass(PATHS.adminDist) : fail('admin/dist/index.html 缺失'))

check('产物', '小程序 H5 构建产物', () =>
  existsSync(join(PATHS.h5Dist, 'index.html')) ? pass(PATHS.h5Dist) : fail('miniapp/dist/build/h5 缺失'))

check('产物', '小程序微信端构建产物', () =>
  existsSync(join(PATHS.mpDist, 'app.json')) ? pass(PATHS.mpDist) : fail('miniapp/dist/build/mp-weixin/app.json 缺失'))

check('产物', '服务端 jar', () =>
  existsSync(PATHS.backendJar) ? pass(PATHS.backendJar) : fail('backend/target 下没有可执行 jar'))

// ------------------------------------------------------ 2. 数据契约一致性
function contractIds() {
  const doc = readFileSync(join(ROOT, 'docs/contract-data.md'), 'utf8')
  const items = new Set(doc.match(/\bfi\d{4}\b/g) ?? [])
  const outfits = new Set(doc.match(/\bof\d{4}\b/g) ?? [])
  return { items, outfits }
}

function seedSql() {
  const sqlPath = join(PATHS.backend, 'src/main/resources/db/data.sql')
  if (!existsSync(sqlPath)) return null
  return readFileSync(sqlPath, 'utf8')
}

check('契约', '种子 SQL 覆盖契约中的全部时装', () => {
  const sql = seedSql()
  if (!sql) return fail('backend/src/main/resources/db/data.sql 不存在')
  const { items } = contractIds()
  const missing = [...items].filter((id) => !sql.includes(id))
  return missing.length ? fail(`data.sql 缺少 ${missing.join(', ')}`) : pass(`${items.size} 个 id 全部命中`)
})

check('契约', '种子 SQL 覆盖契约中的全部搭配', () => {
  const sql = seedSql()
  if (!sql) return fail('data.sql 不存在')
  const { outfits } = contractIds()
  const missing = [...outfits].filter((id) => !sql.includes(id))
  return missing.length ? fail(`data.sql 缺少 ${missing.join(', ')}`) : pass(`${outfits.size} 个 id 全部命中`)
})

check('契约', '字典编码齐备', () => {
  const sql = seedSql()
  if (!sql) return fail('data.sql 不存在')
  const codes = ['fashion_category', 'fashion_rarity', 'fashion_color', 'fashion_style']
  const missing = codes.filter((c) => !sql.includes(c))
  return missing.length ? fail(`缺少字典 ${missing.join(', ')}`) : pass('4 个字典都在')
})

// ------------------------------------------------------------- 3. 数据库
function mysqlQuery(sql) {
  if (!MYSQL) return { ok: false, msg: '未找到 mysql 客户端' }
  const args = [
    `--host=${DB.host}`,
    `--port=${DB.port}`,
    `--user=${DB.user}`,
    `--password=${DB.password}`,
    '--default-character-set=utf8mb4',
    '--skip-column-names',
    '--batch',
    '-e',
    sql,
  ]
  const r = spawnSync(MYSQL, args, { encoding: 'utf8' })
  if (r.status !== 0) {
    const err = (r.stderr || '').split('\n').filter((l) => !/Using a password/i.test(l)).join('\n').trim()
    return { ok: false, msg: err || `mysql 退出码 ${r.status}` }
  }
  return { ok: true, out: (r.stdout || '').trim() }
}

check('数据库', 'MySQL 可连接', () => {
  const r = mysqlQuery('SELECT 1')
  return r.ok ? pass(`${DB.user}@${DB.host}:${DB.port}`) : fail(r.msg)
})

check('数据库', 'fashion_item 有 16 行', () => {
  const r = mysqlQuery(`SELECT COUNT(*) FROM ${DB.name}.fashion_item`)
  if (!r.ok) return fail(r.msg)
  return r.out === '16' ? pass('16 行') : fail(`实际 ${r.out} 行`)
})

check('数据库', 'outfit 有 8 行', () => {
  const r = mysqlQuery(`SELECT COUNT(*) FROM ${DB.name}.outfit`)
  if (!r.ok) return fail(r.msg)
  return r.out === '8' ? pass('8 行') : fail(`实际 ${r.out} 行`)
})

check('数据库', '搭配与时装的关联表已建立', () => {
  const r = mysqlQuery(`SELECT COUNT(*) FROM ${DB.name}.outfit_fashion_item`)
  if (!r.ok) return fail(r.msg)
  const n = Number(r.out)
  return n >= 20 ? pass(`${n} 条关联`) : fail(`只有 ${n} 条关联`)
})

// -------------------------------------------------------------- 4. 接口
async function http(path, options = {}) {
  const res = await fetch(`${API.base}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
  })
  const text = await res.text()
  let json
  try {
    json = JSON.parse(text)
  } catch {
    throw new Error(`非 JSON 响应 (${res.status}): ${text.slice(0, 120)}`)
  }
  return { status: res.status, json }
}

let serverUp = null
async function ensureServer() {
  if (serverUp !== null) return serverUp
  try {
    await http('/app/home')
    serverUp = true
  } catch {
    serverUp = false
  }
  return serverUp
}

check('接口', `服务端可达 :${PORTS.server}`, async () => {
  const r = await ensureServer()
  return r ? pass(API.base) : fail('未启动，先执行 pnpm dev:server')
})

check('接口', '/app/home 结构完整', async () => {
  if (!(await ensureServer())) return fail('服务端未启动')
  const { json } = await http('/app/home')
  const d = json.result || {}
  const need = ['banner', 'categories', 'recommend', 'hotOutfits']
  const missing = need.filter((k) => d[k] === undefined)
  return missing.length ? fail(`缺少字段 ${missing.join(', ')}`) : pass(`${d.recommend.length} 条推荐 / ${d.hotOutfits.length} 套搭配`)
})

check('接口', '后台登录 + 时装分页', async () => {
  if (!(await ensureServer())) return fail('服务端未启动')
  const login = await http('/sys/login', { method: 'POST', body: JSON.stringify({ username: 'admin', password: 'admin123' }) })
  if (!login.json.success) return fail(`登录失败：${login.json.message}`)
  const token = login.json.result?.token
  const list = await http('/fashion/item/list?pageNo=1&pageSize=10', { headers: { 'X-Access-Token': token } })
  const total = list.json.result?.total
  return total === 16 ? pass(`total=16，登录用户 ${login.json.result?.userInfo?.realname}`) : fail(`total=${total}`)
})

check('接口', '筛选与字典', async () => {
  if (!(await ensureServer())) return fail('服务端未启动')
  const filters = await http('/app/fashion/filters')
  const groups = ['categories', 'rarities', 'colors', 'styles']
  const missing = groups.filter((k) => !Array.isArray(filters.json.result?.[k]))
  if (missing.length) return fail(`filters 缺少 ${missing.join(', ')}`)
  const dict = await http('/sys/dict/getDictItems/fashion_rarity')
  const n = dict.json.result?.length ?? 0
  return n >= 5 ? pass(`筛选项 4 组，稀有度字典 ${n} 项`) : fail(`字典项只有 ${n}`)
})

check('接口', '未登录访问后台接口被拒', async () => {
  if (!(await ensureServer())) return fail('服务端未启动')
  const { json } = await http('/fashion/item/list?pageNo=1&pageSize=1')
  return json.code === 401 ? pass('返回 401') : fail(`实际 code=${json.code}`)
})

check('接口', '静态图片可访问', async () => {
  if (!(await ensureServer())) return fail('服务端未启动')
  const res = await fetch(`${API.static}/fashion/fashion-01.jpg`, { method: 'HEAD' })
  return res.ok ? pass(`${res.headers.get('content-type')} ${res.headers.get('content-length')}B`) : fail(`HTTP ${res.status}`)
})

// ---------------------------------------------------------------- 执行
const groups = [...new Set(results.map((r) => r.group))]
console.log(`${C.bold}交付自检${C.reset}\n`)

let failed = 0
for (const group of groups) {
  console.log(`${C.bold}${group}${C.reset}`)
  for (const item of results.filter((r) => r.group === group)) {
    let r
    try {
      r = await item.fn()
    } catch (err) {
      r = fail(err.message)
    }
    if (!r.ok) failed++
    console.log(`  ${r.ok ? `${C.green}✓${C.reset}` : `${C.red}✗${C.reset}`} ${item.name} ${C.dim}${r.msg}${C.reset}`)
  }
  console.log()
}

const total = results.length
if (failed) {
  console.log(`${C.red}${failed}/${total} 项未通过${C.reset}`)
  process.exit(1)
}
console.log(`${C.green}全部 ${total} 项通过${C.reset}`)
