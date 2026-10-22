package com.yanyun.fashion.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yanyun.fashion.entity.FashionItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 时装视图对象：实体字段 + 字典翻译。
 *
 * <p>字典翻译同时给出两种形态：</p>
 * <ul>
 *   <li>{@code category_dictText} / {@code rarity_dictText} —— JeecgBoot 前端表格直显约定；</li>
 *   <li>{@code categoryText} / {@code rarityText} / {@code rarityColor} 等 —— 小程序端直接使用。</li>
 * </ul>
 *
 * @author yanyun backend
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FashionItemVO extends FashionItem {

    private static final long serialVersionUID = 1L;

    /** JeecgBoot 表格直显：部位中文。 */
    @JsonProperty("category_dictText")
    private String categoryDictText;

    /** JeecgBoot 表格直显：稀有度中文。 */
    @JsonProperty("rarity_dictText")
    private String rarityDictText;

    /** 部位中文。 */
    private String categoryText;

    /** 稀有度中文。 */
    private String rarityText;

    /** 稀有度主色。 */
    private String rarityColor;

    /** 稀有度浅底色。 */
    private String rarityBg;

    /** 色系中文。 */
    private String colorText;

    /** 色系主色。 */
    private String colorValue;

    /** 风格中文。 */
    private String styleText;

    /** 标签数组（由逗号分隔的 tags 拆分而来）。 */
    private List<String> tagsList = new ArrayList<String>();
}
