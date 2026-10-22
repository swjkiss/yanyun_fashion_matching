package com.yanyun.fashion.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统用户（JeecgBoot 风格）。
 *
 * @author yanyun backend
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String username;

    /** MD5(用户名 + 明文 + 盐)，小写十六进制。 */
    @JsonIgnore
    private String password;

    @JsonIgnore
    private String salt;

    private String realname;

    private String avatar;

    /** admin / user。 */
    private String role;

    /** 1 正常 0 冻结。 */
    private Integer status;

    @TableLogic
    @JsonIgnore
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
