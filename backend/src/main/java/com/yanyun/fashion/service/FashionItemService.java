package com.yanyun.fashion.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yanyun.fashion.common.BizException;
import com.yanyun.fashion.common.PageResult;
import com.yanyun.fashion.dto.FashionQuery;
import com.yanyun.fashion.entity.FashionItem;
import com.yanyun.fashion.entity.SysDictItem;
import com.yanyun.fashion.mapper.FashionItemMapper;
import com.yanyun.fashion.mapper.OutfitFashionItemMapper;
import com.yanyun.fashion.vo.FashionItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 时装图鉴服务。
 *
 * <p>后台与小程序共用同一套查询构造，差异仅在"是否强制只看上架"。字典翻译统一在
 * {@link #toVO(FashionItem)} 里完成，避免各控制器各写一遍。</p>
 *
 * @author yanyun backend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FashionItemService {

    /** 后台排序字段白名单：请求字段 → 真实列名，避免 SQL 注入。 */
    private static final Map<String, String> SORT_WHITELIST;

    /** 稀有度排序权重（传说在前）。 */
    private static final String RARITY_ORDER =
            "FIELD(rarity, 'legend', 'epic', 'rare', 'fine', 'common')";

    static {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("name", "name");
        map.put("code", "code");
        map.put("sort", "sort");
        map.put("status", "status");
        map.put("viewCount", "view_count");
        map.put("likeCount", "like_count");
        map.put("releaseDate", "release_date");
        map.put("createTime", "create_time");
        map.put("updateTime", "update_time");
        SORT_WHITELIST = Collections.unmodifiableMap(map);
    }

    private final FashionItemMapper itemMapper;

    private final OutfitFashionItemMapper outfitItemMapper;

    private final DictService dictService;

    // ------------------------------------------------------------------ 查询

    /**
     * 分页查询。
     *
     * @param query 查询条件
     * @param app   是否小程序场景（强制只看上架、固定排序规则）
     * @return 分页结果
     */
    public PageResult<FashionItemVO> page(FashionQuery query, boolean app) {
        QueryWrapper<FashionItem> wrapper = new QueryWrapper<FashionItem>();

        if (app) {
            wrapper.eq("status", 1);
        } else if (query.getStatus() != null) {
            wrapper.eq("status", query.getStatus());
        }
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq("category", query.getCategory().trim());
        }
        if (StringUtils.hasText(query.getRarity())) {
            wrapper.eq("rarity", query.getRarity().trim());
        }
        if (StringUtils.hasText(query.getColorFamily())) {
            wrapper.eq("color_family", query.getColorFamily().trim());
        }
        if (StringUtils.hasText(query.getStyle())) {
            wrapper.eq("style", query.getStyle().trim());
        }
        if (query.getIsRecommend() != null) {
            wrapper.eq("is_recommend", query.getIsRecommend());
        }
        if (StringUtils.hasText(query.getName())) {
            String name = query.getName().trim();
            wrapper.and(w -> w.like("name", name).or().like("code", name).or().like("series", name));
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like("name", kw).or().like("series", kw)
                    .or().like("tags", kw).or().like("summary", kw));
        }

        applyOrder(wrapper, query, app);

        Page<FashionItem> page = query.toPage();
        Page<FashionItem> result = itemMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getSize(), result.getCurrent(),
                toVOList(result.getRecords()));
    }

    /** 排序策略：小程序按业务语义，后台按白名单列。 */
    private void applyOrder(QueryWrapper<FashionItem> wrapper, FashionQuery query, boolean app) {
        if (app) {
            String sort = StringUtils.hasText(query.getSort()) ? query.getSort().trim() : "latest";
            if ("hot".equals(sort)) {
                wrapper.orderByDesc("view_count").orderByDesc("like_count").orderByDesc("sort");
            } else if ("rarity".equals(sort)) {
                wrapper.last("ORDER BY " + RARITY_ORDER + ", sort DESC, create_time DESC");
            } else {
                wrapper.orderByDesc("is_recommend").orderByDesc("sort").orderByDesc("create_time");
            }
            return;
        }
        String column = query.safeColumn(SORT_WHITELIST, "sort");
        boolean asc = query.ascending();
        wrapper.orderBy(true, asc, column);
        if (!"sort".equals(column)) {
            wrapper.orderByDesc("sort");
        }
        wrapper.orderByDesc("create_time");
    }

    /**
     * 按主键取详情（不计数）。
     *
     * @param id 时装ID
     * @return 视图对象
     */
    public FashionItemVO getById(String id) {
        FashionItem item = requireItem(id);
        return toVO(item);
    }

    /**
     * 小程序详情：浏览量 +1。
     *
     * @param id 时装ID
     * @return 视图对象
     */
    public FashionItemVO detailAndCount(String id) {
        FashionItem item = requireItem(id);
        itemMapper.incrementViewCount(id);
        item.setViewCount((item.getViewCount() == null ? 0 : item.getViewCount()) + 1);
        return toVO(item);
    }

    /**
     * 批量按ID取（保持传入顺序）。
     *
     * @param ids 时装ID列表
     * @return 视图对象列表
     */
    public List<FashionItemVO> byIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<FashionItemVO>();
        }
        List<FashionItem> items = itemMapper.selectBatchIds(ids);
        Map<String, FashionItem> indexed = new LinkedHashMap<String, FashionItem>();
        for (FashionItem item : items) {
            indexed.put(item.getId(), item);
        }
        List<FashionItemVO> list = new ArrayList<FashionItemVO>();
        for (String id : ids) {
            FashionItem item = indexed.get(id);
            if (item != null) {
                list.add(toVO(item));
            }
        }
        return list;
    }

    /**
     * 推荐时装。
     *
     * @param limit 条数
     * @return 视图对象列表
     */
    public List<FashionItemVO> recommend(int limit) {
        QueryWrapper<FashionItem> wrapper = new QueryWrapper<FashionItem>();
        wrapper.eq("status", 1).eq("is_recommend", 1)
                .orderByDesc("sort").orderByDesc("view_count")
                .last("LIMIT " + Math.max(1, Math.min(limit, 50)));
        return toVOList(itemMapper.selectList(wrapper));
    }

    /**
     * 同部位 / 同色系的相关时装。
     *
     * @param item  当前时装
     * @param limit 条数
     * @return 视图对象列表
     */
    public List<FashionItemVO> related(FashionItem item, int limit) {
        if (item == null) {
            return new ArrayList<FashionItemVO>();
        }
        List<FashionItem> list = itemMapper.selectRelated(item.getId(), item.getCategory(),
                item.getColorFamily(), Math.max(1, Math.min(limit, 20)));
        return toVOList(list);
    }

    // ------------------------------------------------------------------ 写入

    /**
     * 新增时装。
     *
     * @param form 表单
     * @return 新建后的视图对象
     */
    @Transactional(rollbackFor = Exception.class)
    public FashionItemVO create(FashionItem form) {
        validate(form, true);
        form.setId(nextId());
        if (!StringUtils.hasText(form.getCode())) {
            form.setCode(String.format("YY-FW-%03d", itemMapper.selectMaxSeq() == null
                    ? 1 : itemMapper.selectMaxSeq() + 1));
        }
        applyDefaults(form);
        try {
            itemMapper.insert(form);
        } catch (DuplicateKeyException e) {
            throw new BizException("时装编码 " + form.getCode() + " 已存在，请更换");
        }
        log.info("[时装] 新增 {} {}", form.getId(), form.getName());
        return toVO(form);
    }

    /**
     * 修改时装。
     *
     * @param form 表单（必须含 id）
     * @return 修改后的视图对象
     */
    @Transactional(rollbackFor = Exception.class)
    public FashionItemVO update(FashionItem form) {
        if (!StringUtils.hasText(form.getId())) {
            throw new BizException("缺少时装ID");
        }
        requireItem(form.getId());
        validate(form, false);
        applyDefaults(form);
        try {
            itemMapper.updateById(form);
        } catch (DuplicateKeyException e) {
            throw new BizException("时装编码 " + form.getCode() + " 已存在，请更换");
        }
        log.info("[时装] 修改 {} {}", form.getId(), form.getName());
        return toVO(requireItem(form.getId()));
    }

    /**
     * 上下架。
     *
     * @param id     时装ID
     * @param status 1 上架 0 下架
     */
    public void updateStatus(String id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态值非法");
        }
        requireItem(id);
        FashionItem patch = new FashionItem();
        patch.setId(id);
        patch.setStatus(status);
        itemMapper.updateById(patch);
    }

    /**
     * 删除单条（同时清理搭配关联）。
     *
     * @param id 时装ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        requireItem(id);
        outfitItemMapper.deleteByItemId(id);
        itemMapper.deleteById(id);
        log.info("[时装] 删除 {}", id);
    }

    /**
     * 批量删除。
     *
     * @param ids 逗号分隔的时装ID
     * @return 实际删除条数
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(String ids) {
        List<String> list = splitIds(ids);
        if (list.isEmpty()) {
            throw new BizException("请选择要删除的时装");
        }
        for (String id : list) {
            outfitItemMapper.deleteByItemId(id);
        }
        int affected = itemMapper.deleteBatchIds(list);
        log.info("[时装] 批量删除 {} 条", affected);
        return affected;
    }

    // -------------------------------------------------------------- 筛选选项

    /**
     * 小程序筛选面板选项（含各维度计数）。
     *
     * @return 选项结构
     */
    public Map<String, Object> filters() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("categories", options(DictService.CATEGORY, itemMapper.countOnlineGroupByCategory()));
        result.put("rarities", options(DictService.RARITY, itemMapper.countOnlineGroupByRarity()));
        result.put("colors", options(DictService.COLOR, itemMapper.countOnlineGroupByColor()));
        result.put("styles", options(DictService.STYLE, itemMapper.countOnlineGroupByStyle()));
        result.put("sorts", Arrays.asList(
                option("latest", "最新", null, null),
                option("hot", "最热", null, null),
                option("rarity", "稀有度", null, null)));
        return result;
    }

    /** 字典项 + 计数合并成选项列表。 */
    private List<Map<String, Object>> options(String dictCode, List<Map<String, Object>> counts) {
        Map<String, Long> countMap = new LinkedHashMap<String, Long>();
        if (counts != null) {
            for (Map<String, Object> row : counts) {
                Object name = row.get("name");
                Object value = row.get("value");
                if (name != null) {
                    countMap.put(String.valueOf(name), value == null ? 0L : ((Number) value).longValue());
                }
            }
        }
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (SysDictItem item : dictService.items(dictCode)) {
            Long count = countMap.get(item.getItemValue());
            list.add(option(item.getItemValue(), item.getItemText(), item.getItemColor(),
                    count == null ? 0L : count));
        }
        return list;
    }

    private Map<String, Object> option(String value, String text, String color, Long count) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("value", value);
        row.put("text", text);
        if (color != null) {
            row.put("color", color);
        }
        if (count != null) {
            row.put("count", count);
        }
        return row;
    }

    // ------------------------------------------------------------------ 转换

    /**
     * 实体 → 视图对象，补齐字典翻译与标签数组。
     *
     * @param item 实体
     * @return 视图对象
     */
    public FashionItemVO toVO(FashionItem item) {
        if (item == null) {
            return null;
        }
        FashionItemVO vo = new FashionItemVO();
        BeanUtils.copyProperties(item, vo);

        String categoryText = dictService.text(DictService.CATEGORY, item.getCategory());
        String rarityText = dictService.text(DictService.RARITY, item.getRarity());
        vo.setCategoryText(categoryText);
        vo.setCategoryDictText(categoryText);
        vo.setRarityText(rarityText);
        vo.setRarityDictText(rarityText);
        vo.setRarityColor(dictService.color(DictService.RARITY, item.getRarity()));
        vo.setRarityBg(softColor(vo.getRarityColor()));
        vo.setColorText(dictService.text(DictService.COLOR, item.getColorFamily()));
        vo.setColorValue(dictService.color(DictService.COLOR, item.getColorFamily()));
        vo.setStyleText(dictService.text(DictService.STYLE, item.getStyle()));
        vo.setTagsList(splitTags(item.getTags()));
        return vo;
    }

    /**
     * 批量转换。
     *
     * @param items 实体列表
     * @return 视图对象列表
     */
    public List<FashionItemVO> toVOList(List<FashionItem> items) {
        List<FashionItemVO> list = new ArrayList<FashionItemVO>();
        if (items != null) {
            for (FashionItem item : items) {
                list.add(toVO(item));
            }
        }
        return list;
    }

    private List<String> splitTags(String tags) {
        List<String> list = new ArrayList<String>();
        if (StringUtils.hasText(tags)) {
            for (String tag : tags.split("[,，]")) {
                if (StringUtils.hasText(tag)) {
                    list.add(tag.trim());
                }
            }
        }
        return list;
    }

    /** 主色 → 极浅底色（8% 透明度叠加在纸色上的近似值）。 */
    private String softColor(String hex) {
        if (!StringUtils.hasText(hex) || !hex.startsWith("#") || hex.length() != 7) {
            return null;
        }
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            int br = 247;
            int bg = 242;
            int bb = 233;
            int mix = 12;
            r = (r * mix + br * (100 - mix)) / 100;
            g = (g * mix + bg * (100 - mix)) / 100;
            b = (b * mix + bb * (100 - mix)) / 100;
            return String.format("#%02X%02X%02X", r, g, b);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ 工具

    /**
     * 取实体，不存在抛业务异常。
     *
     * @param id 主键
     * @return 实体
     */
    public FashionItem requireItem(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BizException("缺少时装ID");
        }
        FashionItem item = itemMapper.selectById(id);
        if (item == null) {
            throw BizException.notFound("时装不存在或已删除");
        }
        return item;
    }

    private String nextId() {
        Integer max = itemMapper.selectMaxSeq();
        int next = max == null ? 1001 : max + 1;
        return "fi" + next;
    }

    private void validate(FashionItem form, boolean creating) {
        if (form == null) {
            throw new BizException("提交内容为空");
        }
        if (!StringUtils.hasText(form.getName())) {
            throw new BizException("请填写时装名称");
        }
        if (!StringUtils.hasText(form.getCategory())) {
            throw new BizException("请选择时装部位");
        }
        if (!StringUtils.hasText(form.getRarity())) {
            throw new BizException("请选择稀有度");
        }
        if (creating && !StringUtils.hasText(form.getImageUrl())) {
            throw new BizException("请上传或填写时装主图");
        }
    }

    private void applyDefaults(FashionItem form) {
        if (!StringUtils.hasText(form.getGender())) {
            form.setGender("unisex");
        }
        if (form.getStatus() == null || (form.getStatus() != 0 && form.getStatus() != 1)) {
            form.setStatus(1);
        }
        if (form.getSort() == null) {
            form.setSort(0);
        }
        if (form.getViewCount() == null) {
            form.setViewCount(0);
        }
        if (form.getLikeCount() == null) {
            form.setLikeCount(0);
        }
        if (form.getIsRecommend() == null) {
            form.setIsRecommend(0);
        }
        if (!StringUtils.hasText(form.getGlyph()) && StringUtils.hasText(form.getName())) {
            form.setGlyph(form.getName().substring(0, 1));
        }
        if (!StringUtils.hasText(form.getColorFamily())) {
            form.setColorFamily("ink");
        }
        if (!StringUtils.hasText(form.getStyle())) {
            form.setStyle("jianghu");
        }
    }

    private List<String> splitIds(String ids) {
        List<String> list = new ArrayList<String>();
        if (StringUtils.hasText(ids)) {
            for (String id : ids.split(",")) {
                if (StringUtils.hasText(id)) {
                    list.add(id.trim());
                }
            }
        }
        return list;
    }
}
