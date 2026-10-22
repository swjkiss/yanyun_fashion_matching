package com.yanyun.fashion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 字典项（docs/contract-data.md 2.2）。
 *
 * @author yanyun backend
 */
@Data
@TableName("sys_dict_item")
public class SysDictItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String dictId;

    private String itemText;

    private String itemValue;

    /** 色值：稀有度主色 / 色系主色，可为空。 */
    private String itemColor;

    private Integer sortOrder;

    /** 1 启用 0 停用。 */
    private Integer status;

    private Date createTime;
}
