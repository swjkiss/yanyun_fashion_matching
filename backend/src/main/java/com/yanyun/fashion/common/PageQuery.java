package com.yanyun.fashion.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 分页 + 排序基类，供各查询 DTO 继承。
 *
 * <p>请求参数：pageNo（从 1 开始）、pageSize、column（排序字段）、order（asc|desc）。</p>
 *
 * @author yanyun backend
 */
@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 默认每页条数。 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 单页最大条数，防止恶意放大。 */
    public static final int MAX_PAGE_SIZE = 200;

    private Integer pageNo;

    private Integer pageSize;

    /** 排序字段（白名单校验后使用）。 */
    private String column;

    /** asc / desc。 */
    private String order;

    public int currentPage() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public int pageLimit() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public boolean ascending() {
        return "asc".equalsIgnoreCase(order);
    }

    /**
     * 构造 MyBatis-Plus 分页对象。
     *
     * @param <T> 实体类型
     * @return 分页对象
     */
    public <T> Page<T> toPage() {
        return new Page<T>(currentPage(), pageLimit());
    }

    /**
     * 在排序白名单中解析真实列名，未命中则回退默认列，避免 SQL 注入。
     *
     * @param whitelist 请求字段名 → 真实列名
     * @param fallback  默认列名
     * @return 真实列名
     */
    public String safeColumn(Map<String, String> whitelist, String fallback) {
        if (column == null || column.trim().isEmpty()) {
            return fallback;
        }
        String mapped = whitelist.get(column.trim());
        return mapped == null ? fallback : mapped;
    }
}
