# 燕云阁 · 管理后台（Vue3 + Ant Design Vue）

JeecgBoot v3 风格后台：运营总览、时装图鉴维护、搭配方案审核、收藏记录管理。

## 技术栈

- Vue 3.5 + Pinia + Vue Router 4
- Ant Design Vue 4.2（组件库统一交互语言）
- lucide-vue-next + @ant-design/icons-vue（图标）
- Vite 5 + TypeScript（vue-tsc 全量类型检查通过）

## 快速开始

```bash
# 依赖安装（工作区 pnpm）
pnpm --dir admin install

# 开发（要求后端已启动；代理自动转发 /jeecg-boot 与 /images）
pnpm --dir admin run dev
# → http://127.0.0.1:5174

# 生产构建 + 类型检查
pnpm --dir admin run build
pnpm --dir admin run typecheck
```

登录账号：`admin / admin123`（令牌存 localStorage，失效自动回登录页）。

## 设计要点

- **令牌体系**：`admin/src/styles/tokens.css` 由根目录 `scripts/gen-tokens.mjs` 从 `design/tokens.json` 生成；组件内禁止硬编码颜色/间距/圆角。
- **字典与视觉映射**：`src/utils/dict.ts` 内置 4 组业务字典 + 稀有度/色系变量映射，后端字典接口可用时可覆盖。
- **API 客户端**：`src/api/client.ts` 统一封装 JeecgBoot 响应解包、401 跳登录、multipart 上传。
- **虚拟表格**：时装/搭配列表均为分页表格 + 抽屉表单编辑，保持与 Jeecg 一致的交互习惯。

## 目录

```
admin/src/
├─ layouts/AdminLayout.vue   # 侧栏 + 顶栏布局
├─ views/
│  ├─ login/      # 登录页
│  ├─ dashboard/  # 四卡片概览 + 7 日趋势 + 分布 + Top5
│  ├─ items/      # 时装图鉴 CRUD（含上传）
│  ├─ outfits/    # 搭配 CRUD（含审核）
│  └─ favorites/  # 收藏记录
├─ api/           # axios 封装 + 资源 API
├─ stores/        # auth store
└─ utils/         # 字典 / 格式化
```
