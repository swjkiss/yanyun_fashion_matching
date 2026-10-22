package com.yanyun.fashion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanyun.fashion.entity.SysDictItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 字典项 Mapper。
 *
 * @author yanyun backend
 */
@Mapper
public interface SysDictItemMapper extends BaseMapper<SysDictItem> {

    /**
     * 按字典ID查询启用的字典项，按排序升序。
     *
     * @param dictId 字典ID
     * @return 字典项列表
     */
    @Select("SELECT * FROM sys_dict_item WHERE dict_id = #{dictId} AND status = 1 ORDER BY sort_order ASC, id ASC")
    List<SysDictItem> selectByDictId(@Param("dictId") String dictId);
}
