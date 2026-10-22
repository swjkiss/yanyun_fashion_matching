package com.yanyun.fashion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanyun.fashion.entity.Outfit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 搭配方案 Mapper。
 *
 * @author yanyun backend
 */
@Mapper
public interface OutfitMapper extends BaseMapper<Outfit> {

    /**
     * 主键最大序号，用于生成 of#### 业务ID。
     *
     * @return 最大序号，无数据返回 null
     */
    @Select("SELECT MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)) FROM outfit")
    Integer selectMaxSeq();

    /**
     * 点赞数原子自增。
     *
     * @param id 搭配ID
     * @return 影响行数
     */
    @Update("UPDATE outfit SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") String id);

    /**
     * 收藏数原子自增。
     *
     * @param id 搭配ID
     * @return 影响行数
     */
    @Update("UPDATE outfit SET fav_count = fav_count + 1 WHERE id = #{id}")
    int incrementFavCount(@Param("id") String id);

    /**
     * 收藏数原子自减（下限 0，避免脏数据出现负数）。
     *
     * @param id 搭配ID
     * @return 影响行数
     */
    @Update("UPDATE outfit SET fav_count = CASE WHEN fav_count > 0 THEN fav_count - 1 ELSE 0 END WHERE id = #{id}")
    int decrementFavCount(@Param("id") String id);

    /**
     * 浏览数原子自增。
     *
     * @param id 搭配ID
     * @return 影响行数
     */
    @Update("UPDATE outfit SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") String id);

    /**
     * 依据关联表重算件数（保证 item_count 与明细一致）。
     *
     * @param id 搭配ID
     * @return 影响行数
     */
    @Update("UPDATE outfit SET item_count = "
            + "(SELECT COUNT(*) FROM outfit_fashion_item WHERE outfit_id = #{id}) WHERE id = #{id}")
    int refreshItemCount(@Param("id") String id);

    /**
     * 浏览数合计。
     *
     * @return 合计值
     */
    @Select("SELECT IFNULL(SUM(view_count), 0) FROM outfit")
    long sumViewCount();

    /**
     * 按天统计新增搭配数量。
     *
     * @param start 起始时间
     * @return [{d: yyyy-MM-dd, c: count}]
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS d, COUNT(*) AS c FROM outfit "
            + "WHERE create_time >= #{start} GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')")
    List<Map<String, Object>> countDailySince(@Param("start") Date start);
}
