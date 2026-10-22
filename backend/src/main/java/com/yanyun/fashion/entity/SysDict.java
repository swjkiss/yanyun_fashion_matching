package com.yanyun.fashion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据字典（docs/contract-data.md 2.2）。
 *
 * @author yanyun backend
 */
@Data
@TableName("sys_dict")
public class SysDict implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String dictName;

    /** 字典编码，如 fashion_category。 */
    private String dictCode;

    private String description;

    @TableLogic
    @JsonIgnore
    private Integer delFlag;

    private Date createTime;
}
