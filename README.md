# 燕云十六声 · 古风时装搭配

> 一套完整的古风时装图鉴与搭配应用：**微信小程序（uniapp）** + **管理后台（Ant Design Vue）** + **服务端（Spring Boot / JeecgBoot 规范 + MySQL）**。
> 视觉语言取自「墨、纸、金、朱砂」——旧纸为底、暖墨为字、鎏金为线、朱砂为点。

---

## 1. 功能

### 小程序端
| 模块 | 能力 |
|---|---|
| 时装图鉴 | 沉浸式首页、部位分类入口、今日推荐、热门搭配瀑布流 |
| 条件检索 | 关键词 + 部位 / 稀有度 / 色系 / 风格 多维筛选，排序（最新 / 最热 / 稀有度），筛选抽屉 + 已选条件 chip |
| 时装详情 | 大图、稀有度与色系标识、属性表、背景故事、相关搭配推荐 |
| 搭配广场 | 最新 / 最热切换，点赞、收藏就地交互 |
| 搭配详情 | 组成时装列表可下钻、点赞收藏、**canvas 2d 生成分享海报**、`onShareAppMessage` |
| 创建搭配 | 从图鉴选衣（最多 6 件）、风格场景标注、封面自动生成、发表 |
| 我的 | 用户卡、数据统计、我的搭配、我的收藏、清缓存 |

### 管理后台
| 模块 | 能力 |
|---|---|
| 登录 | 账号密码 + 演示账号，JWT 会话 |
| 概览 | 时装/搭配/收藏统计、部位与稀有度分布、色系分布、7 日趋势、热门搭配 Top5 |
| 图鉴管理 | 全量 CRUD、搜索筛选、上下架、批量删除、图片上传、分页 |
| 搭配管理 | 全量 CRUD、多选时装、审核状态、置顶、封面维护 |
| 收藏记录 | 按用户/搭配查询、删除 |

---

## 2. 目录结构

```
.
├─ design/
│  └─ tokens.json              # 设计令牌唯一真值（色板/字号/间距/圆角/阴影/动效）
├─ docs/
│  ├─ contract-data.md         # 数据契约：表结构、枚举字典、种子数据清单
│  ├─ contract-api.md          # 接口契约：统一响应体、分页、全部接口
│  ├─ design-system.md         # 设计系统：主张、色板、字体、间距、动效、组件规范
│  └─ 技术选型说明.md           # 为什么用这些、为什么没用推荐清单里的那些
├─ scripts/
│  ├─ gen-tokens.mjs           # tokens.json → 后台 CSS 变量 + 小程序 SCSS 变量
│  ├─ gen_art.py               # 程序化生成全部视觉素材（Pillow + numpy）
│  ├─ gen_tabbar_icons.py      # tabBar 图标
│  ├─ env.mjs                  # 工具链路径解析（node/pnpm/java/maven/mysql）
│  ├─ dev.mjs                  # 一键启动三端
│  ├─ build.mjs                # 一键构建三端
│  └─ verify.mjs              # 交付自检（产物/契约/数据库/接口）
├─ backend/                    # Spring Boot 2.7 + MyBatis-Plus + MySQL
│  ├─ src/main/java/com/yanyun/fashion/
│  │  ├─ common/               # Result / PageResult / 异常处理
│  │  ├─ config/               # MyBatis-Plus、CORS、静态资源、数据初始化
│  │  ├─ security/             # JWT、拦截器、登录上下文
│  │  ├─ entity/ mapper/ service/ controller/
│  │  └─ storage/              # 本地文件上传
│  ├─ src/main/resources/db/   # schema.sql + data.sql（幂等，启动自动执行）
│  └─ upload/images/           # 静态图片（fashion / outfit / avatar / uploads）
├─ admin/                      # Vue 3.5 + Vite 5 + Ant Design Vue 4 + Pinia
└─ miniapp/                    # uniapp Vue 3 + uni-ui（H5 + 微信小程序双端）
```

---

## 3. 环境要求

| 依赖 | 版本 | 说明 |
|---|---|---|
| JDK | 8 | Spring Boot 2.7 线 |
| Maven | 3.8+ | 后端构建 |
| MySQL | 8.0 | `127.0.0.1:3306`，`root/root`，库名 `yanyun_fashion`（自动创建） |
| Node | ≥ 20 | 前端构建（**不要用系统的 Node 16**） |
| pnpm | 11 | 已固定在 `packageManager` 字段 |
| Python | 3.11+ | 仅用于生成素材，需 Pillow、numpy |

> 本机路径若与默认不同，通过 `JAVA_HOME`、`MAVEN_HOME`、`DSH_PNPM`、`DSH_PYTHON`、`MYSQL_CMD` 覆盖，
> 解析逻辑集中在 `scripts/env.mjs`。

---

## 4. 快速开始

```bash
# 0. 装依赖（仓库根目录）
pnpm install

# 1. 生成设计令牌（两端样式变量的来源）
pnpm tokens

# 2. 生成视觉素材（16 时装 + 6 搭配 + 主视觉 + 头像）
pnpm art

# 3. 启动三端（后端 8080 / 后台 5174 / 小程序 H5 5175）
pnpm dev

# 或者分别启动
pnpm dev:server
pnpm dev:admin
pnpm dev:h5
```

| 服务 | 地址 |
|---|---|
| API | http://127.0.0.1:8080/jeecg-boot |
| 静态图 | http://127.0.0.1:8080/images/... |
| 管理后台 | http://127.0.0.1:5174 |
| 小程序 H5 | http://127.0.0.1:5175 |
| 微信小程序 | `pnpm dev:mp` 后用微信开发者工具打开 `miniapp/dist/dev/mp-weixin` |

**演示账号**

| 场景 | 账号 | 密码 |
|---|---|---|
| 后台 | `admin` | `admin123` |
| 小程序 | `demo` | `demo1234`（启动时自动静默登录） |

数据库无需手工建：后端启动时通过 JDBC 参数自动建库，并执行幂等的 `schema.sql` / `data.sql`。

---

## 5. 构建与自检

```bash
pnpm build          # 令牌 → 后台 → 小程序 H5 → 微信端 → 后端 jar
pnpm build:admin    # 只构建后台
pnpm build:h5
pnpm build:mp
pnpm build:server

pnpm verify         # 交付自检：产物齐备 / 契约一致 / 数据库行数 / 接口连通
```

`pnpm verify` 会用真实请求打后端（登录、列表、筛选、401 拦截、静态图），
任何一项没过都会以非零码退出，可直接接 CI。

---

## 6. 设计系统

- **单一真值**：`design/tokens.json` → `pnpm tokens` → `admin/src/styles/tokens.css`（CSS 变量）+ `miniapp/src/styles/tokens.scss`（SCSS 变量，长度单位 ×2 → rpx）。
- **配色**：纸 `#F7F2E9` 底、墨 `#1B1815` 字、金 `#B08A45` 线、朱砂 `#A6322A` 点；八色系与五级稀有度各有独立色值。
- **字体**：衬线（宋/楷）承担"古"，无衬线承担"今"；小程序走系统字体族降级，不引远程字体。
- **动效**：只用 `opacity` / `transform`，时长 140 / 220 / 380ms，缓动 `cubic-bezier(.22,1,.36,1)`。
- **克制**：不加渐变按钮、不加发光、不加多层投影；装饰元素同屏不超过 2 种。

详见 [设计系统](docs/design-system.md)。

---

## 7. 关于 jeecg-boot

后端**遵循 JeecgBoot 的工程规范与接口契约**：

- 上下文路径 `/jeecg-boot`
- 统一响应体 `{ success, message, code, result, timestamp }`
- 分页体 `{ records, total, size, current, pages }`，参数 `pageNo` / `pageSize` / `column` / `order`
- 鉴权请求头 `X-Access-Token`，JWT
- 字典接口 `/sys/dict/getDictItems/{dictCode}`
- 口令算法 `MD5(username + password + salt)`
- 文件上传 `/sys/common/upload`
- Controller / Service / Mapper 分层 + MyBatis-Plus

**没有直接引入 JeecgBoot 单体**，因为它携带代码生成器、在线报表、工作流、消息中心、Redis 会话、Shiro 权限等大量模块，
而本项目只有三张业务表、两类角色，引入完整单体将带来数十 MB 无关依赖，与"严格控制依赖"的要求冲突。
接口形态保持完全一致——若未来迁回官方单体，前端零改动。详见 [技术选型说明](docs/技术选型说明.md)。

---

## 8. 素材说明

本机没有可用的图像生成模型，因此**全部视觉素材由 `scripts/gen_art.py` 程序化绘制**：
宣纸底噪、色场渐变、墨晕、六种服装剪影（贝塞尔采样 + 4 倍超采样）、金线内框、竖排单字、底部渐暗。
随机种子取 id 哈希，结果可复现、无版权风险、风格绝对统一。

---

## 9. 文档索引

| 文档 | 内容 |
|---|---|
| [数据契约](docs/contract-data.md) | 表结构、枚举字典、16 件时装与 8 套搭配的种子数据 |
| [接口契约](docs/contract-api.md) | 统一响应体、鉴权、全部后台与小程序接口 |
| [设计系统](docs/design-system.md) | 设计主张、令牌、组件规范 |
| [技术选型说明](docs/技术选型说明.md) | 选型理由与推荐清单的逐条判定 |
| [后端说明](backend/README.md) | 环境变量、启动、接口一览 |
| [后台说明](admin/README.md) | 目录结构、页面清单 |
| [小程序说明](miniapp/README.md) | 目录结构、双端差异 |
