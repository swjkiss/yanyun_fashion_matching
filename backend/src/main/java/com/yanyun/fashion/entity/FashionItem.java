package com.yanyun.fashion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * 时装图鉴（docs/contract-data.md 2.3）。
 *
 * @author yanyun backend
 */
@Data
@TableName("fashion_item")
public class FashionItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;

    /** 时装编码 YY-FW-###。 */
    private String code;

    /** 卡片大字。 */
    private String glyph;

    /** 部位，见 fashion_category。 */
    private String category;

    /** 稀有度，见 fashion_rarity。 */
    private String rarity;

    /** 色系，见 fashion_color。 */
    private String colorFamily;

    /** 风格，见 fashion_style。 */
    private String style;

    private String series;

    /** female / male / unisex。 */
    private String gender;

    private String imageUrl;

    private String summary;

    private String description;

    private String story;

    /** 逗号分隔标签。 */
    private String tags;

    private String obtainMethod;

    private String obtainDetail;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;

    /** 1 上架 0 下架。 */
    private Integer status;

    /** 越大越靠前。 */
    private Integer sort;

    private Integer viewCount;

    private Integer likeCount;

    /** 1 推荐。 */
    private Integer isRecommend;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
