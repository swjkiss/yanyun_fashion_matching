-- ============================================================================
-- 燕云十六声 · 古风时装搭配  数据库结构
-- 契约来源：docs/contract-data.md 第 2 节（表结构）
-- 幂等设计：全部 CREATE TABLE IF NOT EXISTS，删库后重启即可自愈
-- ============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 2.1 sys_user（JeecgBoot 风格系统用户）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`          varchar(32)  NOT NULL                COMMENT '主键，u####',
  `username`    varchar(64)  NOT NULL                COMMENT '登录名',
  `password`    varchar(128) NOT NULL                COMMENT 'MD5(用户名+明文+盐)，小写十六进制',
  `salt`        varchar(32)  NOT NULL DEFAULT ''     COMMENT '盐值',
  `realname`    varchar(64)  DEFAULT NULL            COMMENT '昵称',
  `avatar`      varchar(255) DEFAULT NULL            COMMENT '头像相对路径',
  `role`        varchar(32)  NOT NULL DEFAULT 'user' COMMENT 'admin / user',
  `status`      tinyint      NOT NULL DEFAULT 1      COMMENT '1 正常 0 冻结',
  `del_flag`    tinyint      NOT NULL DEFAULT 0      COMMENT '0 正常 1 删除',
  `create_time` datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_time` datetime     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统用户';

-- ---------------------------------------------------------------------------
-- 2.2 sys_dict / sys_dict_item
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict` (
  `id`          varchar(32)  NOT NULL           COMMENT '主键',
  `dict_name`   varchar(100) DEFAULT NULL       COMMENT '字典名称',
  `dict_code`   varchar(100) NOT NULL           COMMENT '字典编码',
  `description` varchar(255) DEFAULT NULL       COMMENT '描述',
  `del_flag`    tinyint      NOT NULL DEFAULT 0 COMMENT '0 正常 1 删除',
  `create_time` datetime     DEFAULT NULL       COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_code` (`dict_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据字典';

CREATE TABLE IF NOT EXISTS `sys_dict_item` (
  `id`          varchar(32)  NOT NULL                COMMENT '主键',
  `dict_id`     varchar(32)  NOT NULL                COMMENT '字典ID',
  `item_text`   varchar(100) NOT NULL                COMMENT '字典项文本',
  `item_value`  varchar(100) NOT NULL                COMMENT '字典项值',
  `item_color`  varchar(32)  DEFAULT NULL            COMMENT '色值（稀有度主色 / 色系主色）',
  `sort_order`  int          NOT NULL DEFAULT 0      COMMENT '排序',
  `status`      tinyint      NOT NULL DEFAULT 1      COMMENT '1 启用 0 停用',
  `create_time` datetime     DEFAULT NULL            COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_dict_item_dict_id` (`dict_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典项';

-- ---------------------------------------------------------------------------
-- 2.3 fashion_item（时装图鉴）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fashion_item` (
  `id`            varchar(32)   NOT NULL           COMMENT '主键，fi####',
  `name`          varchar(100)  NOT NULL           COMMENT '时装名称',
  `code`          varchar(64)   NOT NULL           COMMENT '时装编码 YY-FW-###',
  `glyph`         varchar(4)    DEFAULT NULL       COMMENT '卡片大字',
  `category`      varchar(32)   NOT NULL           COMMENT '部位，见 fashion_category',
  `rarity`        varchar(32)   NOT NULL           COMMENT '稀有度，见 fashion_rarity',
  `color_family`  varchar(32)   DEFAULT NULL       COMMENT '色系，见 fashion_color',
  `style`         varchar(32)   DEFAULT NULL       COMMENT '风格，见 fashion_style',
  `series`        varchar(64)   DEFAULT NULL       COMMENT '系列名',
  `gender`        varchar(16)   NOT NULL DEFAULT 'unisex' COMMENT 'female / male / unisex',
  `image_url`     varchar(255)  DEFAULT NULL       COMMENT '主图相对路径',
  `summary`       varchar(120)  DEFAULT NULL       COMMENT '一句话卖点',
  `description`   varchar(1000) DEFAULT NULL       COMMENT '详情描述',
  `story`         varchar(1000) DEFAULT NULL       COMMENT '背景故事',
  `tags`          varchar(255)  DEFAULT NULL       COMMENT '逗号分隔标签',
  `obtain_method` varchar(100)  DEFAULT NULL       COMMENT '获取途径',
  `obtain_detail` varchar(255)  DEFAULT NULL       COMMENT '获取说明',
  `release_date`  date          DEFAULT NULL       COMMENT '上线日期',
  `status`        tinyint       NOT NULL DEFAULT 1 COMMENT '1 上架 0 下架',
  `sort`          int           NOT NULL DEFAULT 0 COMMENT '越大越靠前',
  `view_count`    int           NOT NULL DEFAULT 0 COMMENT '浏览数',
  `like_count`    int           NOT NULL DEFAULT 0 COMMENT '点赞数',
  `is_recommend`  tinyint       NOT NULL DEFAULT 0 COMMENT '1 推荐',
  `create_by`     varchar(64)   DEFAULT NULL       COMMENT '创建人',
  `create_time`   datetime      DEFAULT NULL       COMMENT '创建时间',
  `update_time`   datetime      DEFAULT NULL       COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fashion_item_code` (`code`),
  KEY `idx_fashion_item_category` (`category`),
  KEY `idx_fashion_item_rarity` (`rarity`),
  KEY `idx_fashion_item_color` (`color_family`),
  KEY `idx_fashion_item_status_sort` (`status`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '时装图鉴';

-- ---------------------------------------------------------------------------
-- 2.4 outfit（搭配方案）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `outfit` (
  `id`            varchar(32)   NOT NULL           COMMENT '主键，of####',
  `title`         varchar(100)  NOT NULL           COMMENT '标题',
  `subtitle`      varchar(120)  DEFAULT NULL       COMMENT '副标题',
  `description`   varchar(1000) DEFAULT NULL       COMMENT '描述',
  `author_id`     varchar(32)   DEFAULT NULL       COMMENT '作者用户ID',
  `author_name`   varchar(64)   DEFAULT NULL       COMMENT '作者昵称（冗余）',
  `author_avatar` varchar(255)  DEFAULT NULL       COMMENT '作者头像（冗余）',
  `cover_image`   varchar(255)  DEFAULT NULL       COMMENT '封面图相对路径',
  `item_count`    int           NOT NULL DEFAULT 0 COMMENT '冗余件数',
  `style_tag`     varchar(32)   DEFAULT NULL       COMMENT '风格，见 fashion_style',
  `color_tone`    varchar(32)   DEFAULT NULL       COMMENT '色调，见 fashion_color',
  `scene_tag`     varchar(32)   DEFAULT NULL       COMMENT '场景标签',
  `like_count`    int           NOT NULL DEFAULT 0 COMMENT '点赞数',
  `fav_count`     int           NOT NULL DEFAULT 0 COMMENT '收藏数',
  `view_count`    int           NOT NULL DEFAULT 0 COMMENT '浏览数',
  `status`        tinyint       NOT NULL DEFAULT 0 COMMENT '1 已发布 0 待审核 2 已下架',
  `is_top`        tinyint       NOT NULL DEFAULT 0 COMMENT '1 置顶',
  `create_time`   datetime      DEFAULT NULL       COMMENT '创建时间',
  `update_time`   datetime      DEFAULT NULL       COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_outfit_author` (`author_id`),
  KEY `idx_outfit_status_top` (`status`, `is_top`),
  KEY `idx_outfit_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '搭配方案';

-- ---------------------------------------------------------------------------
-- 2.5 outfit_fashion_item（搭配—时装 关联）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `outfit_fashion_item` (
  `id`        bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `outfit_id` varchar(32) NOT NULL                COMMENT '搭配ID',
  `item_id`   varchar(32) NOT NULL                COMMENT '时装ID',
  `sort_no`   int         NOT NULL DEFAULT 0      COMMENT '排序',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outfit_item` (`outfit_id`, `item_id`),
  KEY `idx_ofi_item_id` (`item_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '搭配-时装关联';

-- ---------------------------------------------------------------------------
-- 2.6 outfit_favorite（收藏）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `outfit_favorite` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     varchar(32) NOT NULL                COMMENT '用户ID',
  `outfit_id`   varchar(32) NOT NULL                COMMENT '搭配ID',
  `create_time` datetime    DEFAULT NULL            COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_outfit` (`user_id`, `outfit_id`),
  KEY `idx_favorite_outfit_id` (`outfit_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '搭配收藏';
