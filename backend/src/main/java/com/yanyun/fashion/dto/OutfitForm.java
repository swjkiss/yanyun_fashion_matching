package com.yanyun.fashion.dto;

import com.yanyun.fashion.entity.Outfit;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 搭配新增 / 修改表单：实体字段 + 组成时装ID。
 *
 * <p>关联表 {@code outfit_fashion_item} 才是真值，{@code itemIds} 只是入参载体。</p>
 *
 * @author yanyun backend
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OutfitForm extends Outfit {

    private static final long serialVersionUID = 1L;

    /** 组成时装ID（有序，最多 6 件）。 */
    private List<String> itemIds = new ArrayList<String>();
}
