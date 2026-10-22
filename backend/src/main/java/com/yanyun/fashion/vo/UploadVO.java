package com.yanyun.fashion.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 上传结果（docs/contract-api.md 第 7 节）。
 *
 * @author yanyun backend
 */
@Data
public class UploadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 可直接用于 img src 的相对路径，如 /images/uploads/xxx.jpg。 */
    private String url;

    /** 原始文件名。 */
    private String fileName;
}
