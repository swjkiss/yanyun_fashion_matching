import { defineConfig } from 'vite'
// 包本身是 CJS 且动过 ESM 互操作：ESM 导入会得到 {default: {default: fn}}（含 __esModule 标记）
import * as uniPlugin from '@dcloudio/vite-plugin-uni'

const uniMod = uniPlugin.default || uniPlugin
const uni = typeof uniMod === 'function' ? uniMod : uniMod.default

export default defineConfig({
  plugins: [uni()],
})
