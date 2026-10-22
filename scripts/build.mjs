#!/usr/bin/env node
/**
 * 生产构建：设计令牌 → 管理后台 → 小程序 H5 → 小程序微信 → Java 服务端。
 *
 * 用法：
 *   node scripts/build.mjs                 # 全量
 *   node scripts/build.mjs --only admin    # 只构建后台（tokens 会自动带跑）
 *   node scripts/build.mjs --skip server   # 跳过后端
 *
 * 每一步都是真实子进程，失败即中断并打印该步尾部输出，避免"看起来成功"。
 */
import { spawn } from 'node:child_process'
import { existsSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { MAVEN, NODE, PATHS, PNPM_MJS, ROOT, childEnv } from './env.mjs'

const argv = process.argv.slice(2)
const readFlag = (flag) => {
  const i = argv.indexOf(flag)
  return i >= 0 ? argv[i + 1] : null
}
const only = readFlag('--only')
const skip = (readFlag('--skip') || '').split(',').filter(Boolean)

const C = { dim: '\x1b[2m', bold: '\x1b[1m', green: '\x1b[32m', red: '\x1b[31m', cyan: '\x1b[36m', reset: '\x1b[0m' }

function dirSize(dir) {
  if (!existsSync(dir)) return 0
  let total = 0
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const p = join(dir, entry.name)
    if (entry.isDirectory()) total += dirSize(p)
    else if (entry.isFile()) total += statSync(p).size
  }
  return total
}

const mb = (bytes) => `${(bytes / 1024 / 1024).toFixed(2)} MB`

function exec(command, args, cwd) {
  return new Promise((resolve, reject) => {
    const isCmd = /\.(cmd|bat)$/i.test(command)
    // cmd 转发时把 command 与 args 合并为单个 /c 参数；node 会对带引号 arg 再加一层引号，
    // 导致 `\"...\"` 被 cmd 当成文件名（mvn.cmd 实测踩过）。
    // 而 .cmd 路径无空格，直接裸传 + 后续参数单独传即可。
    const child = isCmd
      ? spawn('cmd.exe', ['/d', '/s', '/c', command, ...args], { cwd, env: childEnv() })
      : spawn(command, args, { cwd, env: childEnv() })
    const tail = []
    const keep = (chunk) => {
      tail.push(chunk.toString())
      if (tail.length > 40) tail.shift()
    }
    child.stdout.on('data', keep)
    child.stderr.on('data', keep)
    child.on('error', reject)
    child.on('exit', (code) => {
      if (code === 0) return resolve()
      console.error(`${C.red}--- 失败步骤输出（尾部） ---${C.reset}`)
      console.error(tail.join(''))
      reject(new Error(`${command} ${args.join(' ')} 退出码 ${code}`))
    })
  })
}

const STEPS = [
  {
    key: 'tokens',
    title: '设计令牌',
    run: () => exec(NODE, [join(ROOT, 'scripts/gen-tokens.mjs')], ROOT),
  },
  {
    key: 'admin',
    title: '管理后台（vue-tsc + vite build）',
    dist: PATHS.adminDist,
    run: () => exec(NODE, [PNPM_MJS, '--dir', 'admin', 'run', 'build'], ROOT),
  },
  {
    key: 'h5',
    title: '小程序 H5',
    dist: PATHS.h5Dist,
    run: () => exec(NODE, [PNPM_MJS, '--dir', 'miniapp', 'run', 'build:h5'], ROOT),
  },
  {
    key: 'mp',
    title: '小程序 微信端',
    dist: PATHS.mpDist,
    run: () => exec(NODE, [PNPM_MJS, '--dir', 'miniapp', 'run', 'build:mp-weixin'], ROOT),
  },
  {
    key: 'server',
    title: 'Java 服务端（mvn package）',
    dist: join(PATHS.backend, 'target'),
    run: () => {
      if (!MAVEN) throw new Error('未找到 Maven')
      return exec(MAVEN, ['-q', '-DskipTests', 'package'], PATHS.backend)
    },
  },
]

const results = []
const t0 = Date.now()

console.log(`${C.bold}构建开始${C.reset} ${C.dim}(node ${process.version})${C.reset}\n`)

for (const step of STEPS) {
  if (only && step.key !== only && !(only === 'admin' && step.key === 'tokens')) continue
  if (skip.includes(step.key)) {
    console.log(`${C.dim}跳过 ${step.title}${C.reset}`)
    continue
  }
  const start = Date.now()
  process.stdout.write(`${C.cyan}▸ ${step.title}${C.reset} … `)
  try {
    await step.run()
    const secs = ((Date.now() - start) / 1000).toFixed(1)
    const size = step.dist && existsSync(step.dist) ? ` ${C.dim}${mb(dirSize(step.dist))}${C.reset}` : ''
    console.log(`${C.green}成功${C.reset} ${C.dim}${secs}s${C.reset}${size}`)
    results.push({ ...step, ok: true, secs })
  } catch (err) {
    console.log(`${C.red}失败${C.reset}`)
    results.push({ ...step, ok: false, err })
    break
  }
}

const failed = results.filter((r) => !r.ok)
console.log(`\n${C.bold}构建结束${C.reset}  用时 ${((Date.now() - t0) / 1000).toFixed(1)}s`)
for (const r of results) {
  console.log(`  ${r.ok ? `${C.green}✓${C.reset}` : `${C.red}✗${C.reset}`} ${r.title}${r.secs ? ` ${C.dim}${r.secs}s${C.reset}` : ''}`)
}
if (failed.length) process.exit(1)
