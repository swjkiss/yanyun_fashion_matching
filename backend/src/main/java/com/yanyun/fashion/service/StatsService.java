package com.yanyun.fashion.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanyun.fashion.entity.FashionItem;
import com.yanyun.fashion.entity.Outfit;
import com.yanyun.fashion.mapper.FashionItemMapper;
import com.yanyun.fashion.mapper.OutfitFavoriteMapper;
import com.yanyun.fashion.mapper.OutfitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务：后台概览与近 N 日趋势。
 *
 * <p>数据量很小，直接走聚合 SQL；不引入缓存，保证后台看到的是实时值。</p>
 *
 * @author yanyun backend
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final FashionItemMapper itemMapper;

    private final OutfitMapper outfitMapper;

    private final OutfitFavoriteMapper favoriteMapper;

    private final DictService dictService;

    /**
     * 概览。
     *
     * @return 统计结构（见 docs/contract-api.md 5.1）
     */
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        long itemTotal = itemMapper.selectCount(new QueryWrapper<FashionItem>());
        long itemOnline = itemMapper.selectCount(new QueryWrapper<FashionItem>().eq("status", 1));
        long outfitTotal = outfitMapper.selectCount(new QueryWrapper<Outfit>());
        long outfitPending = outfitMapper.selectCount(new QueryWrapper<Outfit>().eq("status", 0));
        long favoriteTotal = favoriteMapper.countAll();
        long viewTotal = itemMapper.sumViewCount() + outfitMapper.sumViewCount();

        result.put("itemTotal", itemTotal);
        result.put("itemOnline", itemOnline);
        result.put("outfitTotal", outfitTotal);
        result.put("outfitPending", outfitPending);
        result.put("favoriteTotal", favoriteTotal);
        result.put("viewTotal", viewTotal);

        result.put("categoryDist", translate(itemMapper.countGroupByCategory(), DictService.CATEGORY));
        result.put("rarityDist", translate(itemMapper.countGroupByRarity(), DictService.RARITY));
        result.put("colorDist", translate(itemMapper.countGroupByColor(), DictService.COLOR));
        result.put("topOutfits", topOutfits(5));
        return result;
    }

    /**
     * 近 N 日趋势。
     *
     * @param days 天数（1~30）
     * @return [{date, items, outfits, favorites}]
     */
    public List<Map<String, Object>> trend(int days) {
        int span = Math.max(1, Math.min(days, 30));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -(span - 1));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date start = calendar.getTime();

        Map<String, Long> items = toCountMap(itemMapper.countDailySince(start));
        Map<String, Long> outfits = toCountMap(outfitMapper.countDailySince(start));
        Map<String, Long> favorites = toCountMap(favoriteMapper.countDailySince(start));

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Calendar cursor = Calendar.getInstance();
        cursor.setTime(start);
        for (int i = 0; i < span; i++) {
            String key = format.format(cursor.getTime());
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("date", key);
            row.put("items", items.containsKey(key) ? items.get(key) : 0L);
            row.put("outfits", outfits.containsKey(key) ? outfits.get(key) : 0L);
            row.put("favorites", favorites.containsKey(key) ? favorites.get(key) : 0L);
            list.add(row);
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
        return list;
    }

    /** 热门搭配 TopN。 */
    private List<Map<String, Object>> topOutfits(int limit) {
        QueryWrapper<Outfit> wrapper = new QueryWrapper<Outfit>();
        wrapper.orderByDesc("fav_count").orderByDesc("like_count")
                .last("LIMIT " + Math.max(1, Math.min(limit, 20)));
        List<Outfit> outfits = outfitMapper.selectList(wrapper);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Outfit outfit : outfits) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", outfit.getId());
            row.put("title", outfit.getTitle());
            row.put("favCount", outfit.getFavCount());
            row.put("likeCount", outfit.getLikeCount());
            row.put("coverImage", outfit.getCoverImage());
            list.add(row);
        }
        return list;
    }

    /** 把 [{name,value}] 翻译成 [{name, text, value}]。 */
    private List<Map<String, Object>> translate(List<Map<String, Object>> rows, String dictCode) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (rows == null) {
            return list;
        }
        for (Map<String, Object> row : rows) {
            Object name = row.get("name");
            Object value = row.get("value");
            String code = name == null ? "" : String.valueOf(name);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("name", dictService.text(dictCode, code));
            item.put("code", code);
            item.put("value", value == null ? 0L : ((Number) value).longValue());
            String color = dictService.color(dictCode, code);
            if (StringUtils.hasText(color)) {
                item.put("color", color);
            }
            list.add(item);
        }
        return list;
    }

    private Map<String, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new LinkedHashMap<String, Long>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object d = row.get("d");
            Object c = row.get("c");
            if (d != null) {
                map.put(String.valueOf(d), c == null ? 0L : ((Number) c).longValue());
            }
        }
        return map;
    }
}
