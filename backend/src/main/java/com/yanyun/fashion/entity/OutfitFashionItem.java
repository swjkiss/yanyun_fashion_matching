package com.yanyun.fashion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 搭配—时装 关联（docs/contract-data.md 2.5）。
 *
 * @author yanyun backend
 */
@Data
@TableName("outfit_fashion_item")
public class OutfitFashionItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String outfitId;

    private String itemId;

    private Integer sortNo;

    public OutfitFashionItem() {
    }

    public OutfitFashionItem(String outfitId, String itemId, Integer sortNo) {
        this.outfitId = outfitId;
        this.itemId = itemId;
        this.sortNo = sortNo;
    }
}
