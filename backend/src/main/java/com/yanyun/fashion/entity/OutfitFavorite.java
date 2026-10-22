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
 * 搭配收藏（docs/contract-data.md 2.6）。
 *
 * @author yanyun backend
 */
@Data
@TableName("outfit_favorite")
public class OutfitFavorite implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String outfitId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    public OutfitFavorite() {
    }

    public OutfitFavorite(String userId, String outfitId) {
        this.userId = userId;
        this.outfitId = outfitId;
    }
}
