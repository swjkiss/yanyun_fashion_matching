package com.yanyun.fashion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanyun.fashion.entity.OutfitFashionItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 搭配—时装关联 Mapper。
 *
 * @author yanyun backend
 */
@Mapper
public interface OutfitFashionItemMapper extends BaseMapper<OutfitFashionItem> {

    /**
     * 查询某搭配包含的时装ID（按排序）。
     *
     * @param outfitId 搭配ID
     * @return 时装ID列表
     */
    @Select("SELECT item_id FROM outfit_fashion_item WHERE outfit_id = #{outfitId} ORDER BY sort_no ASC, id ASC")
    List<String> selectItemIds(@Param("outfitId") String outfitId);

    /**
     * 查询包含某时装的搭配ID。
     *
     * @param itemId 时装ID
     * @return 搭配ID列表
     */
    @Select("SELECT outfit_id FROM outfit_fashion_item WHERE item_id = #{itemId}")
    List<String> selectOutfitIdsByItemId(@Param("itemId") String itemId);

    /**
     * 删除某搭配的全部关联。
     *
     * @param outfitId 搭配ID
     * @return 影响行数
     */
    @Delete("DELETE FROM outfit_fashion_item WHERE outfit_id = #{outfitId}")
    int deleteByOutfitId(@Param("outfitId") String outfitId);

    /**
     * 删除某时装的全部关联（时装被删除时调用）。
     *
     * @param itemId 时装ID
     * @return 影响行数
     */
    @Delete("DELETE FROM outfit_fashion_item WHERE item_id = #{itemId}")
    int deleteByItemId(@Param("itemId") String itemId);
}
