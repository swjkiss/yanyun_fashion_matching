/**
 * 本机工具链路径解析。
 *
 * 本项目在 Windows 上开发，Node / pnpm / JDK / Maven / MySQL 的安装位置并不标准，
 * 因此统一在此收敛：优先环境变量，其次 DSH 运行时内置路径，最后常见安装目录。
 * 其余脚本一律从这里取路径，避免各自硬编码。
 */
import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

export const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
export const NODE = process.execPath

const DSH_DEPS = 'C:/Users/Administrator/.dsh/dsh-runtimes/dsh-primary-runtime/dependencies'

const norm = (p) => (p ? p.replace(/\\/g, '/') : null)

function pick(...candidates) {
  for (const c of candidates) if (c && existsSync(c)) return norm(c)
  return null
}

export const PNPM_MJS = pick(
  process.env.DSH_PNPM,
  join(DSH_DEPS, 'pnpm/bin/pnpm.mjs'),
  join(DSH_DEPS, 'pnpm/pnpm.cjs'),
)

export const JAVA_HOME = pick(
  process.env.JAVA_HOME,
  'D:/Program Files/Java/jdk1.8.0_251',
  'C:/Program Files/Java/jdk1.8.0_251',
)

export const MAVEN = pick(
  process.env.MAVEN_CMD,
  process.env.MAVEN_HOME && join(process.env.MAVEN_HOME, 'bin/mvn.cmd'),
  'D:/tools/apache-maven-3.9.8/bin/mvn.cmd',
  'C:/Program Files/apache-maven-3.9.8/bin/mvn.cmd',
)

export const MYSQL = pick(
  process.env.MYSQL_CMD,
  'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe',
)

export const PYTHON = pick(
  process.env.DSH_PYTHON,
  join(DSH_DEPS, 'python/python.exe'),
)

export const DB = {
  host: process.env.DB_HOST || '127.0.0.1',
  port: process.env.DB_PORT || '3306',
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || 'root',
  name: process.env.DB_NAME || 'yanyun_fashion',
}

export const PORTS = { server: 8080, admin: 5174, h5: 5175 }

export const API = {
  host: `http://127.0.0.1:${PORTS.server}`,
  base: `http://127.0.0.1:${PORTS.server}/jeecg-boot`,
  // 静态图片挂在 context-path 下（/jeecg-boot/images/**）
  static: `http://127.0.0.1:${PORTS.server}/jeecg-boot/images`,
}

export const PATHS = {
  backend: join(ROOT, 'backend'),
  backendJar: join(ROOT, 'backend/target/yanyun-fashion-server.jar'),
  images: join(ROOT, 'backend/upload/images'),
  admin: join(ROOT, 'admin'),
  adminDist: join(ROOT, 'admin/dist'),
  miniapp: join(ROOT, 'miniapp'),
  h5Dist: join(ROOT, 'miniapp/dist/build/h5'),
  mpDist: join(ROOT, 'miniapp/dist/build/mp-weixin'),
  tokensJson: join(ROOT, 'design/tokens.json'),
  adminTokens: join(ROOT, 'admin/src/styles/tokens.css'),
  miniTokens: join(ROOT, 'miniapp/src/styles/tokens.scss'),
}

/** 子进程环境：把 JDK / Maven 前置到 PATH，避免 `mvn` 找不到 java。 */
export function childEnv(extra = {}) {
  const env = { ...process.env }
  let path = env.PATH || env.Path || ''
  if (MAVEN) path = `${dirname(MAVEN)};${path}`
  if (JAVA_HOME) {
    env.JAVA_HOME = JAVA_HOME
    path = `${JAVA_HOME}/bin;${path}`
  }
  env.PATH = path
  env.FORCE_COLOR = '1'
  return { ...env, ...extra }
}

/**
 * 启动外部命令。Windows 上 .cmd/.bat 必须经 cmd.exe 转发，
 * 否则 spawn 会 ENOENT（mvn.cmd 就是这种情况）。
 */
export function run(command, args = [], options = {}) {
  if (!command) throw new Error('run(): command 为空，请检查工具链路径')
  const isCmd = /\.(cmd|bat)$/i.test(command)
  return isCmd
    ? spawn('cmd.exe', ['/d', '/s', '/c', `"${command}"`, ...args], {
        stdio: 'inherit',
        ...options,
        env: options.env || childEnv(),
      })
    : spawn(command, args, { stdio: 'inherit', ...options, env: options.env || childEnv() })
}


