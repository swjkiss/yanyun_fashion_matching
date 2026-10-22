package com.yanyun.fashion.dto;

import com.yanyun.fashion.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 搭配查询参数（后台与小程序共用）。
 *
 * @author yanyun backend
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OutfitQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 标题模糊词（后台）。 */
    private String title;

    /** 关键词：标题 / 副标题 / 描述（小程序）。 */
    private String keyword;

    /** 0 待审核 1 已发布 2 已下架，null 表示不限（后台）。 */
    private Integer status;

    private String styleTag;

    private String authorName;

    /** 只看我的（小程序）。 */
    private String authorId;

    /** latest | hot。 */
    private String sort;
}
