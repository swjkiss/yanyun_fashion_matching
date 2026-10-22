package com.yanyun.fashion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanyun.fashion.entity.OutfitFavorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 收藏 Mapper。
 *
 * @author yanyun backend
 */
@Mapper
public interface OutfitFavoriteMapper extends BaseMapper<OutfitFavorite> {

    /**
     * 统计某用户对某搭配的收藏数。
     *
     * @param userId   用户ID
     * @param outfitId 搭配ID
     * @return 记录数
     */
    @Select("SELECT COUNT(*) FROM outfit_favorite WHERE user_id = #{userId} AND outfit_id = #{outfitId}")
    int countByUserAndOutfit(@Param("userId") String userId, @Param("outfitId") String outfitId);

    /**
     * 取消收藏。
     *
     * @param userId   用户ID
     * @param outfitId 搭配ID
     * @return 影响行数
     */
    @Delete("DELETE FROM outfit_favorite WHERE user_id = #{userId} AND outfit_id = #{outfitId}")
    int deleteByUserAndOutfit(@Param("userId") String userId, @Param("outfitId") String outfitId);

    /**
     * 求和收藏数。
     *
     * @return 合计
     */
    @Select("SELECT COUNT(*) FROM outfit_favorite")
    long countAll();

    /**
     * 某搭配被收藏次数。
     *
     * @param outfitId 搭配ID
     * @return 次数
     */
    @Select("SELECT COUNT(*) FROM outfit_favorite WHERE outfit_id = #{outfitId}")
    long countByOutfit(@Param("outfitId") String outfitId);

    /**
     * 某用户收藏过的搭配ID集合（用于列表批量标注 favorited）。
     *
     * @param userId 用户ID
     * @return 搭配ID列表
     */
    @Select("SELECT outfit_id FROM outfit_favorite WHERE user_id = #{userId}")
    List<String> selectOutfitIdsByUser(@Param("userId") String userId);

    /**
     * 后台收藏记录分页（联表取用户昵称与搭配标题）。
     *
     * @param keyword 标题 / 用户昵称模糊词，可空
     * @param offset  偏移
     * @param size    条数
     * @return 行数据
     */
    @Select("<script>"
            + "SELECT f.id AS id, f.user_id AS userId, u.realname AS userName, u.avatar AS userAvatar, "
            + "f.outfit_id AS outfitId, o.title AS outfitTitle, o.cover_image AS coverImage, "
            + "DATE_FORMAT(f.create_time, '%Y-%m-%d %H:%i:%s') AS createTime "
            + "FROM outfit_favorite f "
            + "LEFT JOIN sys_user u ON u.id = f.user_id "
            + "LEFT JOIN outfit o ON o.id = f.outfit_id "
            + "<where>"
            + "<if test=\"keyword != null and keyword != ''\">"
            + "(o.title LIKE CONCAT('%', #{keyword}, '%') OR u.realname LIKE CONCAT('%', #{keyword}, '%') "
            + "OR f.outfit_id LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if>"
            + "</where>"
            + "ORDER BY f.create_time DESC, f.id DESC LIMIT #{offset}, #{size}"
            + "</script>")
    List<Map<String, Object>> selectAdminPage(@Param("keyword") String keyword,
                                              @Param("offset") int offset,
                                              @Param("size") int size);

    /**
     * 后台收藏记录总数。
     *
     * @param keyword 标题 / 用户昵称模糊词，可空
     * @return 总数
     */
    @Select("<script>"
            + "SELECT COUNT(*) FROM outfit_favorite f "
            + "LEFT JOIN sys_user u ON u.id = f.user_id "
            + "LEFT JOIN outfit o ON o.id = f.outfit_id "
            + "<where>"
            + "<if test=\"keyword != null and keyword != ''\">"
            + "(o.title LIKE CONCAT('%', #{keyword}, '%') OR u.realname LIKE CONCAT('%', #{keyword}, '%') "
            + "OR f.outfit_id LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if>"
            + "</where>"
            + "</script>")
    long countAdminPage(@Param("keyword") String keyword);

    /**
     * 按天统计收藏数。
     *
     * @param start 起始时间
     * @return [{d: yyyy-MM-dd, c: count}]
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS d, COUNT(*) AS c FROM outfit_favorite "
            + "WHERE create_time >= #{start} GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')")
    List<Map<String, Object>> countDailySince(@Param("start") Date start);
}
