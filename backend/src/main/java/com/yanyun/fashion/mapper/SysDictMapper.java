package com.yanyun.fashion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanyun.fashion.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 数据字典 Mapper。
 *
 * @author yanyun backend
 */
@Mapper
public interface SysDictMapper extends BaseMapper<SysDict> {

    /**
     * 按字典编码查询。
     *
     * @param dictCode 字典编码
     * @return 字典
     */
    @Select("SELECT * FROM sys_dict WHERE dict_code = #{dictCode} AND del_flag = 0 LIMIT 1")
    SysDict selectByCode(@Param("dictCode") String dictCode);
}
