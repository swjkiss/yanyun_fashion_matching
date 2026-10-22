package com.yanyun.fashion.dto;

import com.yanyun.fashion.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 时装查询参数（后台与小程序共用）。
 *
 * @author yanyun backend
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FashionQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 名称 / 编码 / 系列 模糊词（后台）。 */
    private String name;

    /** 关键词：匹配名称、系列、标签（小程序）。 */
    private String keyword;

    private String category;

    private String rarity;

    private String colorFamily;

    private String style;

    /** 1 上架 0 下架，null 表示不限（后台）。 */
    private Integer status;

    /** 仅看推荐。 */
    private Integer isRecommend;

    /** 小程序排序：latest | hot | rarity。 */
    private String sort;
}
