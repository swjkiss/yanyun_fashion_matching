# 燕云十六声 · 小程序端（uniapp + Vue3）

「时装图鉴 + 搭配圈」双端应用：WeChat 小程序（发行）与 H5（开发预览）。

## 功能

- **图鉴**：首页 hero + 分类宫格 + 今日推荐横滑；检索页关键词 + 组合筛选（稀有度/色系/风格/性别/部位）+ 搜索历史；详情页属性表、背景故事、同系列推荐。
- **搭配圈**：广场（最新/最热）、创建器（选 1-6 件时装、风格场景标签）、详情（点赞/收藏、海报生成、分享）。
- **个人**：我的搭配、收藏夹、游客模式缓存清理。

## 快速开始

```bash
# H5 开发（浏览器 http://127.0.0.1:5173，需后端已启动）
pnpm --dir miniapp run dev:h5

# 微信小程序构建
pnpm --dir miniapp run build:mp-weixin
# → miniapp/dist/build/mp-weixin，用微信开发者工具导入该目录
```

服务端连接：`src/config/index.js` 的 `API_BASE`，默认 `http://127.0.0.1:8080`；上线时改这一个常量即可。

## 结构

```
miniapp/src/
├─ pages/            # 9 页面（3 个 tabBar：图鉴/搭配/我的）
│  ├─ index/    首页
│  ├─ search/   检索
│  ├─ item/     时装详情
│  ├─ outfit/   广场 / 详情 / 创建
│  └─ mine/     我的 / 收藏 / 我的搭配
├─ components/       # InkItemCard / InkOutfitCard（水墨风卡片）
├─ styles/           # tokens.scss（由 gen-tokens.mjs 生成）+ 动效
├─ static/tabbar/    # 生成的水墨风 tabBar 图标
└─ stores/           # pinia（user）
```

## 小程序发包注意

- `manifest.json` 内 mp-weixin `appid` 当前为占位符 `touristappid`，正式发布前替换为真实 appid。
- 分享海报用 canvas 2d + `canvasToTempFilePath`，仅小程序端可用；H5 端 `%ifdef` 跳过并提示进小程序。
