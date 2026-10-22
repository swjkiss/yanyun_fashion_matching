# 数据契约（Data Contract）

> 本文件是 **单一事实来源**：Java 后端建表、后台管理、小程序、程序化插画四处必须与本表一致。
> 修改顺序：本文件 → `backend/src/main/resources/db/*.sql` → 三端适配。

数据库：MySQL 8.0，库名 `yanyun_fashion`，字符集 `utf8mb4`，排序 `utf8mb4_general_ci`。

---

## 1. 枚举字典

### 1.1 时装部位 `fashion_category`

| value | text | 说明 |
|---|---|---|
| `set` | 套装 | 整套成衣 |
| `top` | 上衣 | |
| `bottom` | 下装 | |
| `headwear` | 头饰 | 冠、笠、钗 |
| `accessory` | 配饰 | 披风、云肩、发带 |
| `hairstyle` | 发型 | |

### 1.2 稀有度 `fashion_rarity`

| value | text | 色值 | 底色 |
|---|---|---|---|
| `common` | 凡品 | `#7E8A94` | `#F0F3F4` |
| `fine` | 良品 | `#47655C` | `#E4EDEA` |
| `rare` | 珍品 | `#38485A` | `#E3E8EE` |
| `epic` | 绝品 | `#8A6A32` | `#F6EEDF` |
| `legend` | 传说 | `#A6322A` | `#F7E5E2` |

### 1.3 色系 `fashion_color`（同时用于卡片主视觉取色）

| value | text | 主色 | 浅色 |
|---|---|---|---|
| `ink` | 玄墨 | `#1B1815` | `#D9D0C6` |
| `cinnabar` | 朱砂 | `#A6322A` | `#F7E5E2` |
| `gold` | 鎏金 | `#B08A45` | `#F6EEDF` |
| `moon` | 月白 | `#9AA6AF` | `#F0F3F4` |
| `bamboo` | 竹青 | `#56755A` | `#E5EDE3` |
| `indigo` | 靛青 | `#38485A` | `#E3E8EE` |
| `purple` | 绛紫 | `#6B4159` | `#EFE3EA` |
| `ochre` | 赭石 | `#9A6238` | `#F2E3D6` |

### 1.4 风格 `fashion_style`

| value | text |
|---|---|
| `wuxia` | 侠客 |
| `court` | 庙堂 |
| `scholar` | 文人 |
| `jianghu` | 江湖 |
| `xianxia` | 仙侠 |
| `exotic` | 异域 |

### 1.5 状态
- 时装 `status`：`1` 上架 / `0` 下架
- 搭配 `status`：`1` 已发布 / `0` 待审核 / `2` 已下架

---

## 2. 表结构

### 2.1 `sys_user`（JeecgBoot 风格系统用户）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | varchar(32) PK | |
| `username` | varchar(64) UK | 登录名 |
| `password` | varchar(128) | **MD5(明文+salt)**，与 JeecgBoot 一致 |
| `salt` | varchar(32) | |
| `realname` | varchar(64) | 昵称 |
| `avatar` | varchar(255) | `/images/avatar/avatar-01.jpg` |
| `role` | varchar(32) | `admin` / `user` |
| `status` | tinyint | 1 正常 0 冻结 |
| `del_flag` | tinyint | 0 正常 1 删除 |
| `create_time` | datetime | |
| `update_time` | datetime | |

### 2.2 `sys_dict` / `sys_dict_item`

| sys_dict | |
|---|---|
| `id` | varchar(32) PK |
| `dict_name` | varchar(100) |
| `dict_code` | varchar(100) UK |
| `description` | varchar(255) |
| `del_flag` | tinyint |
| `create_time` | datetime |

| sys_dict_item | |
|---|---|
| `id` | varchar(32) PK |
| `dict_id` | varchar(32) |
| `item_text` | varchar(100) |
| `item_value` | varchar(100) |
| `item_color` | varchar(32) 可空（稀有度色值） |
| `sort_order` | int |
| `status` | tinyint |
| `create_time` | datetime |

### 2.3 `fashion_item`（时装图鉴）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | varchar(32) PK | `fi1001` |
| `name` | varchar(100) | 玄刃·墨鳞甲 |
| `code` | varchar(64) UK | `YY-FW-001` |
| `glyph` | varchar(4) | 卡片大字，单字，如 `玄` |
| `category` | varchar(32) | 见 1.1 |
| `rarity` | varchar(32) | 见 1.2 |
| `color_family` | varchar(32) | 见 1.3 |
| `style` | varchar(32) | 见 1.4 |
| `series` | varchar(64) | 系列名 |
| `gender` | varchar(16) | `female` / `male` / `unisex` |
| `image_url` | varchar(255) | `/images/fashion/fashion-01.jpg` |
| `summary` | varchar(120) | 一句话卖点 |
| `description` | varchar(1000) | 详情描述 |
| `story` | varchar(1000) | 背景故事 |
| `tags` | varchar(255) | 逗号分隔，如 `铠甲,重装,北境` |
| `obtain_method` | varchar(100) | 获取途径 |
| `obtain_detail` | varchar(255) | 获取说明 |
| `release_date` | date | 上线日期 |
| `status` | tinyint | 1 上架 0 下架 |
| `sort` | int | 越大越靠前 |
| `view_count` | int | |
| `like_count` | int | |
| `is_recommend` | tinyint | 1 推荐 |
| `create_by` | varchar(64) | |
| `create_time` | datetime | |
| `update_time` | datetime | |

### 2.4 `outfit`（搭配方案）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | varchar(32) PK | `of2001` |
| `title` | varchar(100) | 江湖夜雨 |
| `subtitle` | varchar(120) | 一身黑衣，半生风雪 |
| `description` | varchar(1000) | |
| `author_id` | varchar(32) | 关联 sys_user.id |
| `author_name` | varchar(64) | 冗余昵称 |
| `author_avatar` | varchar(255) | 冗余头像 |
| `cover_image` | varchar(255) | `/images/outfit/outfit-01.jpg` |
| `item_count` | int | 冗余件数（写入时维护） |
| `style_tag` | varchar(32) | 见 1.4 |
| `color_tone` | varchar(32) | 见 1.3 |
| `scene_tag` | varchar(32) | 夜行 / 朝会 / 雅集 … |
| `like_count` | int | |
| `fav_count` | int | |
| `view_count` | int | |
| `status` | tinyint | 见 1.5 |
| `is_top` | tinyint | 1 置顶 |
| `create_time` | datetime | |
| `update_time` | datetime | |

### 2.5 `outfit_fashion_item`（搭配—时装 关联）

| 字段 | 类型 |
|---|---|
| `id` | bigint AUTO_INCREMENT PK |
| `outfit_id` | varchar(32) |
| `item_id` | varchar(32) |
| `sort_no` | int |

唯一索引 `uk_outfit_item (outfit_id, item_id)`。

### 2.6 `outfit_favorite`（收藏）

| 字段 | 类型 |
|---|---|
| `id` | bigint AUTO_INCREMENT PK |
| `user_id` | varchar(32) |
| `outfit_id` | varchar(32) |
| `create_time` | datetime |

唯一索引 `uk_user_outfit (user_id, outfit_id)`。

---

## 3. 种子数据

### 3.1 用户

| id | username | password | realname | role |
|---|---|---|---|---|
| `u1001` | `admin` | `admin123` | 云中君 | admin |
| `u1002` | `demo` | `demo1234` | 青衫客 | user |
| `u1003` | `suyi` | `suyi1234` | 素衣 | user |

### 3.2 时装（16 件，图片文件名严格对应）

| id | name | code | glyph | category | rarity | color | style | series | image |
|---|---|---|---|---|---|---|---|---|---|
| fi1001 | 玄刃·墨鳞甲 | YY-FW-001 | 玄 | set | epic | ink | wuxia | 玄刃 | fashion-01 |
| fi1002 | 赤霄·朱砂战袍 | YY-FW-002 | 赤 | set | legend | cinnabar | wuxia | 赤霄 | fashion-02 |
| fi1003 | 流金·鎏金纹锦袍 | YY-FW-003 | 金 | set | legend | gold | court | 流金 | fashion-03 |
| fi1004 | 霜刃·月白劲装 | YY-FW-004 | 霜 | set | rare | moon | wuxia | 霜刃 | fashion-04 |
| fi1005 | 松风·竹青直裰 | YY-FW-005 | 松 | top | fine | bamboo | scholar | 松风 | fashion-05 |
| fi1006 | 幽兰·靛青广袖 | YY-FW-006 | 幽 | top | rare | indigo | court | 幽兰 | fashion-06 |
| fi1007 | 素练·月白中衣 | YY-FW-007 | 素 | top | common | moon | scholar | 素练 | fashion-07 |
| fi1008 | 赭石·行旅短打 | YY-FW-008 | 跋 | bottom | common | ochre | jianghu | 行旅 | fashion-08 |
| fi1009 | 青霓·竹青罗裙 | YY-FW-009 | 霓 | bottom | fine | bamboo | xianxia | 青霓 | fashion-09 |
| fi1010 | 寒山·玄墨斗笠 | YY-FW-010 | 笠 | headwear | fine | ink | jianghu | 寒山 | fashion-10 |
| fi1011 | 十六声·鎏金发冠 | YY-FW-011 | 冠 | headwear | epic | gold | court | 十六声 | fashion-11 |
| fi1012 | 绛雪·绛紫披风 | YY-FW-012 | 绛 | accessory | rare | purple | wuxia | 绛雪 | fashion-12 |
| fi1013 | 牵丝·靛青发带 | YY-FW-013 | 牵 | accessory | common | indigo | exotic | 牵丝 | fashion-13 |
| fi1014 | 朱明·朱砂云肩 | YY-FW-014 | 肩 | accessory | epic | cinnabar | exotic | 朱明 | fashion-14 |
| fi1015 | 云髻·月白垂鬟 | YY-FW-015 | 髻 | hairstyle | fine | moon | xianxia | 云髻 | fashion-15 |
| fi1016 | 青丝·玄墨高束 | YY-FW-016 | 丝 | hairstyle | common | ink | jianghu | 青丝 | fashion-16 |

图片路径统一 `/images/fashion/<image>.jpg`（1024×1536，由 `scripts/gen_art.py` 生成）。

### 3.3 搭配（8 套）

| id | title | subtitle | items | style | tone | scene | cover | author |
|---|---|---|---|---|---|---|---|---|
| of2001 | 江湖夜雨 | 一身黑衣，半生风雪 | fi1001,fi1010,fi1016 | wuxia | ink | 夜行 | outfit-01.jpg | u1002 |
| of2002 | 庙堂朱衣 | 满殿金辉，一人独坐 | fi1003,fi1006,fi1011 | court | gold | 朝会 | outfit-02.jpg | u1001 |
| of2003 | 竹间清谈 | 煮茶听风，与谁共语 | fi1005,fi1008,fi1013 | scholar | bamboo | 雅集 | outfit-03.jpg | u1003 |
| of2004 | 雪落云鬓 | 白首同归，天地无声 | fi1007,fi1009,fi1015 | xianxia | moon | 踏雪 | outfit-04.jpg | u1002 |
| of2005 | 金戈铁马 | 赤色当先，万军辟易 | fi1002,fi1004,fi1014 | wuxia | cinnabar | 征战 | outfit-05.jpg | u1001 |
| of2006 | 塞上异域 | 风沙千里，故人不归 | fi1012,fi1013,fi1014 | exotic | purple | 远行 | outfit-06.jpg | u1003 |
| of2007 | 素衣素心 | 一箪一瓢，亦是江山 | fi1005,fi1007,fi1015 | scholar | bamboo | 山居 | *复用* fashion-07.jpg | u1002 |
| of2008 | 朱砂劫 | 恨不相逢，未嫁之时 | fi1002,fi1012,fi1016 | jianghu | cinnabar | 复仇 | *复用* fashion-02.jpg | u1003 |

> `cover_image` 前缀为 `/images/outfit/`；复用项写 `/images/fashion/xxx.jpg`。
> 初始收藏：`u1002` 收藏 of2001、of2004；`u1003` 收藏 of2002。

---

## 4. 命名与约定

- 主键统一字符串业务 ID（`fi####` / `of####` / `u####`），后台新增时后端生成。
- 时间统一 `yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai`。
- 图片一律相对路径 `/images/...`，客户端拼接 `API_BASE`。
- 金额/计数均为整数，不做小数。
