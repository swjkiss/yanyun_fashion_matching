#!/usr/bin/env node
/**
 * 一键启动本地开发环境：Java 服务端 + 管理后台 + 小程序 H5。
 *
 * 用法：
 *   node scripts/dev.mjs                  # 全启
 *   node scripts/dev.mjs --only server    # 只启后端
 *   node scripts/dev.mjs --only admin|h5
 *
 * 三个进程的输出都会加彩色前缀，方便在一屏里区分；Ctrl+C 统一退出。
 */
import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import { MAVEN, NODE, PATHS, PNPM_MJS, PORTS, ROOT, childEnv } from './env.mjs'

const argv = process.argv.slice(2)
const onlyIdx = argv.indexOf('--only')
const only = onlyIdx >= 0 ? argv[onlyIdx + 1] : null

const C = { server: '\x1b[35m', admin: '\x1b[36m', h5: '\x1b[33m', dim: '\x1b[2m', reset: '\x1b[0m' }

const children = []

function spawnService(name, command, args, cwd) {
  if (only && only !== name) return
  const isCmd = /\.(cmd|bat)$/i.test(command)
  const child = isCmd
    ? spawn('cmd.exe', ['/d', '/s', '/c', `"${command}"`, ...args], { cwd, env: childEnv() })
    : spawn(command, args, { cwd, env: childEnv() })
  children.push({ name, child })

  const prefix = `${C[name]}[${name}]${C.reset} `
  const pipe = (stream, isErr) => {
    let buffer = ''
    stream.setEncoding('utf8')
    stream.on('data', (chunk) => {
      buffer += chunk
      const lines = buffer.split(/\r?\n/)
      buffer = lines.pop() ?? ''
      for (const line of lines) {
        if (!line.trim()) continue
        process.stdout.write(prefix + (isErr ? `\x1b[31m${line}\x1b[0m` : line) + '\n')
      }
    })
  }
  pipe(child.stdout, false)
  pipe(child.stderr, true)

  child.on('exit', (code) => {
    console.log(`${prefix}进程退出，code=${code}`)
    if (code !== 0) shutdown(code ?? 1)
  })
  return child
}

let shuttingDown = false
function shutdown(code = 0) {
  if (shuttingDown) return
  shuttingDown = true
  for (const { child } of children) {
    if (child.exitCode === null) child.kill()
  }
  setTimeout(() => process.exit(code), 400)
}

process.on('SIGINT', () => shutdown(0))
process.on('SIGTERM', () => shutdown(0))

// ------------------------------------------------------------------ backend
function startServer() {
  if (!MAVEN) {
    console.error(`${C.server}[server]${C.reset} 未找到 Maven（设置 MAVEN_HOME 或 MAVEN_CMD）`)
    return
  }
  const hasJar = existsSync(PATHS.backendJar)
  if (hasJar) {
    console.log(`${C.server}[server]${C.reset} 检测到已打包 jar，直接运行（改代码后请执行 pnpm build:server）`)
    spawnService('server', 'java', ['-Dfile.encoding=UTF-8', '-jar', PATHS.backendJar], PATHS.backend)
  } else {
    console.log(`${C.server}[server]${C.reset} 未找到 jar，使用 mvn spring-boot:run 启动`)
    spawnService('server', MAVEN, ['-q', '-DskipTests', 'spring-boot:run'], PATHS.backend)
  }
}

// ------------------------------------------------------------------- admin
function startAdmin() {
  spawnService('admin', NODE, [PNPM_MJS, '--dir', 'admin', 'run', 'dev'], ROOT)
}

// ---------------------------------------------------------------------- h5
function startH5() {
  spawnService('h5', NODE, [PNPM_MJS, '--dir', 'miniapp', 'run', 'dev:h5'], ROOT)
}

console.log(`
\x1b[1m燕云十六声 · 时装搭配\x1b[0m  本地开发环境
  API      http://127.0.0.1:${PORTS.server}/jeecg-boot
  管理后台  http://127.0.0.1:${PORTS.admin}
  小程序H5  http://127.0.0.1:${PORTS.h5}
  ${C.dim}Ctrl+C 退出全部进程${C.reset}
`)

startServer()
startAdmin()
startH5()
