# 接口契约（API Contract）

> 后端：Spring Boot 2.7 (Java 8) + MyBatis-Plus + MySQL 8，**遵循 JeecgBoot 规范**。
> 服务地址：`http://127.0.0.1:8080`，上下文路径：`/jeecg-boot`。
> 后台管理调用 `/jeecg-boot/**`，小程序调用 `/jeecg-boot/app/**`。

---

## 0. 通用规范

### 0.1 统一响应体（JeecgBoot `Result<T>`）

```json
{ "success": true, "message": "", "code": 200, "result": {}, "timestamp": 1763000000000 }
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `success` | boolean | 业务是否成功 |
| `message` | string | 失败原因 / 成功提示 |

> **鉴权失败约定**：HTTP 状态恒为 200，响应体 `code` 为 `401`（未登录/过期）或 `403`（非管理员），
> 前端拦截器依据响应体 `code` 处理，不依赖 HTTP 非 2xx。
| `code` | int | 200 成功，500 业务异常，401 未登录 |
| `result` | any | 业务数据 |
| `timestamp` | long | 服务器毫秒时间戳 |

### 0.2 分页结果（JeecgBoot `IPage`）

```json
{ "records": [], "total": 16, "size": 10, "current": 1, "pages": 2 }
```

请求参数：`pageNo`（从 1 开始）、`pageSize`、`column`（排序字段）、`order`（`asc|desc`）。

### 0.3 鉴权

- 登录成功后返回 JWT，前端每次请求携带请求头 `X-Access-Token: <token>`。
- 后台接口（`/sys/**` 除登录、`/fashion/**`）需要 `admin` 角色。
- 小程序接口（`/app/**`，除 `guest-login` 外）需要登录态，任意角色。
- 未登录/过期统一返回 `code: 401`。

### 0.4 错误码

| code | 含义 |
|---|---|
| 200 | 成功 |
| 401 | 未登录或 token 失效 |
| 403 | 无权限 |
| 500 | 业务异常（`message` 为可直接展示的中文） |

---

## 1. 认证 `/jeecg-boot`

### 1.1 后台登录
`POST /sys/login`

```json
{ "username": "admin", "password": "admin123" }
```
→ `result`:
```json
{ "token": "eyJhbGciOi...", "userInfo": { "id": "u1001", "username": "admin", "realname": "云中君", "avatar": "/images/avatar/avatar-01.jpg", "role": "admin" } }
```

### 1.2 获取当前用户
`GET /sys/user/getUserInfo` → `result`: `{ "userInfo": {...}, "roles": ["admin"] }`

### 1.3 退出
`POST /sys/logout` → `result: true`

### 1.4 小程序免密登录（演示用）
`POST /app/guest-login`
```json
{ "username": "demo" }
```
→ `result`: `{ "token": "...", "userInfo": { "id": "u1002", "realname": "青衫客", "avatar": "..." } }`

---

## 2. 数据字典

### 2.1 按编码取字典项（JeecgBoot 原生形态）
`GET /sys/dict/getDictItems/{dictCode}`

```json
{ "success": true, "code": 200,
  "result": [ { "text": "套装", "value": "set", "title": "套装" } ] }
```

可用 `dictCode`：`fashion_category`、`fashion_rarity`、`fashion_color`、`fashion_style`。
稀有度/色系额外返回 `color` 字段（主色值）。

### 2.2 字典列表
`GET /sys/dict/list` → `result`: `[{ "id","dictCode","dictName","itemCount" }]`

---

## 3. 后台·时装管理 `/fashion/item`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/list` | 分页查询 |
| GET | `/queryById?id=` | 详情 |
| POST | `/add` | 新增 |
| PUT | `/edit` | 修改 |
| DELETE | `/delete?id=` | 删除 |
| DELETE | `/deleteBatch?ids=a,b` | 批量删除 |
| PUT | `/status` | 上下架 `{ id, status }` |

`GET /list` 查询参数：`pageNo`、`pageSize`、`name`（模糊）、`category`、`rarity`、`colorFamily`、`style`、`status`、`column`、`order`。

`records[]` 元素为完整 `fashion_item`（见数据契约 2.3），额外带 `category_dictText`、`rarity_dictText` 两个直显文本（JeecgBoot 字典翻译惯例）。

新增/修改请求体即 `fashion_item` 字段（`id` 新增时不传，后端生成）。

---

## 4. 后台·搭配管理 `/fashion/outfit`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/list` | 分页（参数 `title`、`status`、`styleTag`、`authorName`） |
| GET | `/queryById?id=` | 详情，附带 `items: FashionItem[]` |
| POST | `/add` | 新增（`itemIds: string[]`） |
| PUT | `/edit` | 修改（`itemIds: string[]`） |
| DELETE | `/delete?id=` | 删除 |
| DELETE | `/deleteBatch?ids=` | 批量删除 |
| PUT | `/audit` | 审核 `{ id, status }` |

写入时后端维护 `item_count`、`outfit_fashion_item` 关联与 `update_time`。

---

## 5. 后台·收藏与统计

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/fashion/favorite/list` | 收藏记录分页（`outfitTitle`、`userName`） |
| DELETE | `/fashion/favorite/delete?id=` | 删除记录 |
| GET | `/fashion/stats/overview` | 概览 |
| GET | `/fashion/stats/trend` | 近 7 日新增 |

### 5.1 `overview` 返回
```json
{
  "itemTotal": 16, "itemOnline": 14, "outfitTotal": 8, "outfitPending": 1,
  "favoriteTotal": 12, "viewTotal": 86420,
  "categoryDist":   [ { "name": "套装", "value": 4 } ],
  "rarityDist":     [ { "name": "凡品", "value": 4 } ],
  "colorDist":      [ { "name": "玄墨", "value": 3 } ],
  "topOutfits":     [ { "id": "of2001", "title": "江湖夜雨", "favCount": 312, "coverImage": "/images/outfit/outfit-01.jpg" } ]
}
```

### 5.2 `trend` 返回
```json
[ { "date": "2026-10-15", "items": 2, "outfits": 1, "favorites": 3 } ]
```

---

## 6. 小程序 `/jeecg-boot/app`

### 6.1 首页聚合（一次请求拿全）
`GET /app/home`

```json
{
  "banner": { "image": "/images/hero-banner.jpg", "title": "云想衣裳", "subtitle": "十六声 · 时装图鉴" },
  "stats": { "itemTotal": 16, "outfitTotal": 8 },
  "categories": [ { "value": "set", "text": "套装", "count": 4 } ],
  "recommend": [ FashionItem ],
  "hotOutfits": [ Outfit ]
}
```

### 6.2 时装检索
`GET /app/fashion/items`

参数：`keyword`（匹配 name/series/tags）、`category`、`rarity`、`colorFamily`、`style`、`sort`（`latest|hot|rarity`）、`pageNo`、`pageSize`。

`result`: `{ records, total, size, current, pages }`，元素为 `FashionItem` + `categoryText`、`rarityText`、`rarityColor`。

### 6.3 时装详情
`GET /app/fashion/item/{id}` → `result`: `{ item: FashionItem, related: FashionItem[], outfits: Outfit[] }`（每次访问 `view_count + 1`）

### 6.4 筛选面板选项
`GET /app/fashion/filters`

```json
{
  "categories": [ { "value": "set", "text": "套装", "count": 4 } ],
  "rarities":   [ { "value": "epic", "text": "绝品", "color": "#8A6A32", "count": 3 } ],
  "colors":     [ { "value": "ink", "text": "玄墨", "color": "#1B1815", "count": 3 } ],
  "styles":     [ { "value": "wuxia", "text": "侠客", "count": 4 } ],
  "sorts":      [ { "value": "latest", "text": "最新" }, { "value": "hot", "text": "最热" }, { "value": "rarity", "text": "稀有度" } ]
}
```

### 6.5 搭配广场
`GET /app/outfits` 参数 `keyword`、`styleTag`、`sort`（`latest|hot`）、`pageNo`、`pageSize` → 分页 `Outfit`（附 `favorited: boolean`）。

### 6.6 搭配详情
`GET /app/outfit/{id}` → `{ outfit: Outfit, items: FashionItem[], favorited: boolean, author: { nickname, avatar } }`

### 6.7 创建搭配
`POST /app/outfit`

```json
{ "title": "夜行衣", "subtitle": "风起时", "description": "...", "itemIds": ["fi1001","fi1010"], "styleTag": "wuxia", "sceneTag": "夜行", "coverImage": "/images/fashion/fashion-01.jpg" }
```
→ `result`: 新建的 `Outfit`（`status` 默认 1，`author` 取当前登录用户）

### 6.8 收藏切换
`POST /app/favorite/toggle` → `{ "outfitId": "of2001" }` → `result`: `{ "favorited": true, "favCount": 313 }`

### 6.9 我的收藏
`GET /app/favorites?pageNo=&pageSize=` → 分页 `Outfit`

### 6.10 我的搭配
`GET /app/my/outfits?pageNo=&pageSize=` → 分页 `Outfit`

### 6.11 点赞
`POST /app/outfit/{id}/like` → `result`: `{ "likeCount": 893 }`

---

## 7. 文件上传

`POST /sys/common/upload`（`multipart/form-data`，字段名 `file`）

```json
{ "success": true, "message": "上传成功", "code": 200,
  "result": { "url": "/images/uploads/1763000000-a1b2.jpg", "fileName": "xxx.jpg" } }
```
限制：仅 `jpg/jpeg/png/webp`，≤ 5MB，落盘 `backend/upload/images/uploads/`。

---

## 8. 静态资源

`GET /jeecg-boot/images/**` → 映射到 `backend/upload/images/**`（Spring `ResourceHandler`；context-path 生效，图片完整 URL 需带 `/jeecg-boot` 前缀）。
开发期允许跨域：`Access-Control-Allow-Origin: *`，暴露全部方法。
