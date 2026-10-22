#!/usr/bin/env node
/**
 * 调用程序化插画生成器（scripts/gen_art.py）。
 * 单独包一层的原因：确保使用带 Pillow/numpy 的内置 Python，而不是系统里那个裸解释器。
 *
 *   node scripts/art.mjs                 # 全量生成
 *   node scripts/art.mjs --only fashion-01 outfit-02
 */
import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { join } from 'node:path'
import { PYTHON, ROOT } from './env.mjs'

const script = join(ROOT, 'scripts/gen_art.py')
if (!existsSync(script)) {
  console.error('scripts/gen_art.py 不存在')
  process.exit(1)
}
if (!PYTHON) {
  console.error('未找到内置 Python（设置 DSH_PYTHON 环境变量）')
  process.exit(1)
}

const r = spawnSync(PYTHON, [script, ...process.argv.slice(2)], { stdio: 'inherit', cwd: ROOT })
process.exit(r.status ?? 1)
