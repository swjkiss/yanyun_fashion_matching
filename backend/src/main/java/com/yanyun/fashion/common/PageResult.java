package com.yanyun.fashion.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页结果（与 JeecgBoot 的 IPage 序列化结构保持一致）。
 *
 * <pre>
 * { "records": [], "total": 16, "size": 10, "current": 1, "pages": 2 }
 * </pre>
 *
 * @param <T> 记录类型
 * @author yanyun backend
 */
@Data
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页记录。 */
    private List<T> records = new ArrayList<T>();

    /** 总记录数。 */
    private long total;

    /** 每页条数。 */
    private long size;

    /** 当前页码，从 1 开始。 */
    private long current;

    /** 总页数。 */
    private long pages;

    public static <T> PageResult<T> of(long total, long size, long current, List<T> records) {
        PageResult<T> page = new PageResult<T>();
        page.setTotal(total);
        page.setSize(size);
        page.setCurrent(current);
        page.setRecords(records == null ? new ArrayList<T>() : records);
        page.setPages(size <= 0 ? 0 : (total + size - 1) / size);
        return page;
    }

    public static <T> PageResult<T> empty(long size, long current) {
        return of(0L, size, current, new ArrayList<T>());
    }
}
