package com.yanyun.fashion.vo;

import com.yanyun.fashion.entity.Outfit;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 搭配视图对象：实体字段 + 字典翻译 + 组成时装 + 当前用户收藏态。
 *
 * @author yanyun backend
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OutfitVO extends Outfit {

    private static final long serialVersionUID = 1L;

    /** 风格中文。 */
    private String styleText;

    /** 色调中文。 */
    private String colorText;

    /** 色调主色。 */
    private String colorValue;

    /** 状态中文：已发布 / 待审核 / 已下架。 */
    private String statusText;

    /** 组成时装ID（按排序）。 */
    private List<String> itemIds = new ArrayList<String>();

    /** 组成时装明细（详情 / 编辑时返回）。 */
    private List<FashionItemVO> items = new ArrayList<FashionItemVO>();

    /** 当前登录用户是否已收藏（未登录为 null）。 */
    private Boolean favorited;
}
