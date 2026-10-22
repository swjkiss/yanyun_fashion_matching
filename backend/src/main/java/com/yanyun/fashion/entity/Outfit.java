package com.yanyun.fashion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 搭配方案（docs/contract-data.md 2.4）。
 *
 * @author yanyun backend
 */
@Data
@TableName("outfit")
public class Outfit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String title;

    private String subtitle;

    private String description;

    private String authorId;

    /** 冗余作者昵称。 */
    private String authorName;

    /** 冗余作者头像。 */
    private String authorAvatar;

    private String coverImage;

    /** 冗余件数（写入时维护）。 */
    private Integer itemCount;

    /** 风格，见 fashion_style。 */
    private String styleTag;

    /** 色调，见 fashion_color。 */
    private String colorTone;

    /** 场景标签：夜行 / 朝会 / 雅集 … */
    private String sceneTag;

    private Integer likeCount;

    private Integer favCount;

    private Integer viewCount;

    /** 1 已发布 0 待审核 2 已下架。 */
    private Integer status;

    /** 1 置顶。 */
    private Integer isTop;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
