package com.yanyun.fashion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanyun.fashion.entity.FashionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 时装图鉴 Mapper。
 *
 * @author yanyun backend
 */
@Mapper
public interface FashionItemMapper extends BaseMapper<FashionItem> {

    /**
     * 主键最大序号，用于生成 fi#### 业务ID。
     *
     * @return 最大序号，无数据返回 null
     */
    @Select("SELECT MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)) FROM fashion_item")
    Integer selectMaxSeq();

    /**
     * 浏览数原子自增。
     *
     * @param id 时装ID
     * @return 影响行数
     */
    @Update("UPDATE fashion_item SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") String id);

    /**
     * 按部位统计（含下架）。
     *
     * @return [{name: category, value: count}]
     */
    @Select("SELECT category AS name, COUNT(*) AS value FROM fashion_item GROUP BY category ORDER BY value DESC")
    List<Map<String, Object>> countGroupByCategory();

    /**
     * 按部位统计（仅上架）。
     *
     * @return [{name: category, value: count}]
     */
    @Select("SELECT category AS name, COUNT(*) AS value FROM fashion_item WHERE status = 1 "
            + "GROUP BY category ORDER BY value DESC")
    List<Map<String, Object>> countOnlineGroupByCategory();

    /**
     * 按稀有度统计（含下架）。
     *
     * @return [{name: rarity, value: count}]
     */
    @Select("SELECT rarity AS name, COUNT(*) AS value FROM fashion_item GROUP BY rarity ORDER BY value DESC")
    List<Map<String, Object>> countGroupByRarity();

    /**
     * 按稀有度统计（仅上架）。
     *
     * @return [{name: rarity, value: count}]
     */
    @Select("SELECT rarity AS name, COUNT(*) AS value FROM fashion_item WHERE status = 1 "
            + "GROUP BY rarity ORDER BY value DESC")
    List<Map<String, Object>> countOnlineGroupByRarity();

    /**
     * 按色系统计（含下架）。
     *
     * @return [{name: colorFamily, value: count}]
     */
    @Select("SELECT color_family AS name, COUNT(*) AS value FROM fashion_item GROUP BY color_family ORDER BY value DESC")
    List<Map<String, Object>> countGroupByColor();

    /**
     * 按色系统计（仅上架）。
     *
     * @return [{name: colorFamily, value: count}]
     */
    @Select("SELECT color_family AS name, COUNT(*) AS value FROM fashion_item WHERE status = 1 "
            + "GROUP BY color_family ORDER BY value DESC")
    List<Map<String, Object>> countOnlineGroupByColor();

    /**
     * 按风格统计（仅上架）。
     *
     * @return [{name: style, value: count}]
     */
    @Select("SELECT style AS name, COUNT(*) AS value FROM fashion_item WHERE status = 1 "
            + "GROUP BY style ORDER BY value DESC")
    List<Map<String, Object>> countOnlineGroupByStyle();

    /**
     * 浏览数合计。
     *
     * @return 合计值
     */
    @Select("SELECT IFNULL(SUM(view_count), 0) FROM fashion_item")
    long sumViewCount();

    /**
     * 按天统计新增数量。
     *
     * @param start 起始时间
     * @return [{d: yyyy-MM-dd, c: count}]
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS d, COUNT(*) AS c FROM fashion_item "
            + "WHERE create_time >= #{start} GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')")
    List<Map<String, Object>> countDailySince(@Param("start") Date start);

    /**
     * 关联推荐：同部位或同色系的上架时装。
     *
     * @param id          当前时装ID
     * @param category    部位
     * @param colorFamily 色系
     * @param limit       条数
     * @return 推荐列表
     */
    @Select("SELECT * FROM fashion_item WHERE status = 1 AND id <> #{id} "
            + "AND (category = #{category} OR color_family = #{colorFamily}) "
            + "ORDER BY is_recommend DESC, sort DESC LIMIT #{limit}")
    List<FashionItem> selectRelated(@Param("id") String id,
                                    @Param("category") String category,
                                    @Param("colorFamily") String colorFamily,
                                    @Param("limit") int limit);
}
