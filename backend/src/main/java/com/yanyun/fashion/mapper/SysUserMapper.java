package com.yanyun.fashion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanyun.fashion.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统用户 Mapper。
 *
 * @author yanyun backend
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按登录名查询（忽略逻辑删除）。
     *
     * @param username 登录名
     * @return 用户
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND del_flag = 0 LIMIT 1")
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 按主键批量查询。
     *
     * @param ids 用户ID集合
     * @return 用户列表
     */
    @Select("<script>SELECT * FROM sys_user WHERE del_flag = 0 AND id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<SysUser> selectByIds(@Param("ids") List<String> ids);
}
