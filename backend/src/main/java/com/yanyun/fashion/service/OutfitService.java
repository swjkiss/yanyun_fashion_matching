package com.yanyun.fashion.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yanyun.fashion.common.BizException;
import com.yanyun.fashion.common.PageResult;
import com.yanyun.fashion.dto.OutfitForm;
import com.yanyun.fashion.dto.OutfitQuery;
import com.yanyun.fashion.entity.Outfit;
import com.yanyun.fashion.entity.OutfitFavorite;
import com.yanyun.fashion.entity.OutfitFashionItem;
import com.yanyun.fashion.mapper.OutfitFavoriteMapper;
import com.yanyun.fashion.mapper.OutfitFashionItemMapper;
import com.yanyun.fashion.mapper.OutfitMapper;
import com.yanyun.fashion.security.LoginUser;
import com.yanyun.fashion.security.UserContext;
import com.yanyun.fashion.vo.FashionItemVO;
import com.yanyun.fashion.vo.OutfitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 搭配服务：列表 / 详情 / 创建 / 审核 / 点赞 / 收藏。
 *
 * <p>收藏与点赞都走"原子 SQL + 事务"，避免并发下计数与明细不一致。</p>
 *
 * @author yanyun backend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitService {

    /** 单套搭配最多包含的时装件数。 */
    private static final int MAX_ITEMS = 6;

    private static final Map<String, String> SORT_WHITELIST;

    static {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("title", "title");
        map.put("status", "status");
        map.put("itemCount", "item_count");
        map.put("likeCount", "like_count");
        map.put("favCount", "fav_count");
        map.put("viewCount", "view_count");
        map.put("isTop", "is_top");
        map.put("createTime", "create_time");
        map.put("updateTime", "update_time");
        SORT_WHITELIST = Collections.unmodifiableMap(map);
    }

    private final OutfitMapper outfitMapper;

    private final OutfitFashionItemMapper outfitItemMapper;

    private final OutfitFavoriteMapper favoriteMapper;

    private final FashionItemService fashionItemService;

    private final DictService dictService;

    // ------------------------------------------------------------------ 查询

    /**
     * 分页查询。
     *
     * @param query  查询条件
     * @param app    是否小程序场景
     * @param userId 当前用户ID（用于标注 favorited，可空）
     * @return 分页结果
     */
    public PageResult<OutfitVO> page(OutfitQuery query, boolean app, String userId) {
        QueryWrapper<Outfit> wrapper = new QueryWrapper<Outfit>();

        if (app) {
            wrapper.eq("status", 1);
        } else if (query.getStatus() != null) {
            wrapper.eq("status", query.getStatus());
        }
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like("title", query.getTitle().trim());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like("title", kw).or().like("subtitle", kw).or().like("description", kw));
        }
        if (StringUtils.hasText(query.getStyleTag())) {
            wrapper.eq("style_tag", query.getStyleTag().trim());
        }
        if (StringUtils.hasText(query.getAuthorName())) {
            wrapper.like("author_name", query.getAuthorName().trim());
        }
        if (StringUtils.hasText(query.getAuthorId())) {
            wrapper.eq("author_id", query.getAuthorId().trim());
        }

        if (app) {
            String sort = StringUtils.hasText(query.getSort()) ? query.getSort().trim() : "latest";
            if ("hot".equals(sort)) {
                wrapper.orderByDesc("like_count").orderByDesc("fav_count").orderByDesc("view_count");
            } else {
                wrapper.orderByDesc("is_top").orderByDesc("create_time");
            }
        } else {
            String column = query.safeColumn(SORT_WHITELIST, "create_time");
            wrapper.orderBy(true, query.ascending(), column);
            if (!"is_top".equals(column)) {
                wrapper.orderByDesc("is_top");
            }
            if (!"create_time".equals(column)) {
                wrapper.orderByDesc("create_time");
            }
        }

        Page<Outfit> page = query.toPage();
        Page<Outfit> result = outfitMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getSize(), result.getCurrent(),
                toVOList(result.getRecords(), userId, false));
    }

    /**
     * 详情。
     *
     * @param id     搭配ID
     * @param userId 当前用户ID，可空
     * @param count  是否累加浏览量
     * @return 视图对象
     */
    public OutfitVO detail(String id, String userId, boolean count) {
        Outfit outfit = requireOutfit(id);
        if (count) {
            outfitMapper.incrementViewCount(id);
            outfit.setViewCount((outfit.getViewCount() == null ? 0 : outfit.getViewCount()) + 1);
        }
        List<OutfitVO> list = toVOList(Collections.singletonList(outfit), userId, true);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 我的搭配。
     *
     * @param userId 用户ID
     * @param pageNo 页码
     * @param size   每页条数
     * @return 分页结果
     */
    public PageResult<OutfitVO> myOutfits(String userId, int pageNo, int size) {
        QueryWrapper<Outfit> wrapper = new QueryWrapper<Outfit>();
        wrapper.eq("author_id", userId).orderByDesc("create_time");
        Page<Outfit> page = new Page<Outfit>(pageNo, size);
        Page<Outfit> result = outfitMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getSize(), result.getCurrent(),
                toVOList(result.getRecords(), userId, false));
    }

    /**
     * 我的收藏。
     *
     * @param userId 用户ID
     * @param pageNo 页码
     * @param size   每页条数
     * @return 分页结果
     */
    public PageResult<OutfitVO> myFavorites(String userId, int pageNo, int size) {
        QueryWrapper<OutfitFavorite> wrapper = new QueryWrapper<OutfitFavorite>();
        wrapper.eq("user_id", userId).orderByDesc("create_time").orderByDesc("id");
        Page<OutfitFavorite> page = new Page<OutfitFavorite>(pageNo, size);
        Page<OutfitFavorite> favorites = favoriteMapper.selectPage(page, wrapper);

        List<String> ids = new ArrayList<String>();
        for (OutfitFavorite favorite : favorites.getRecords()) {
            ids.add(favorite.getOutfitId());
        }
        List<Outfit> outfits = ids.isEmpty()
                ? new ArrayList<Outfit>()
                : outfitMapper.selectBatchIds(ids);
        Map<String, Outfit> indexed = new LinkedHashMap<String, Outfit>();
        for (Outfit outfit : outfits) {
            indexed.put(outfit.getId(), outfit);
        }
        List<Outfit> ordered = new ArrayList<Outfit>();
        for (String id : ids) {
            Outfit outfit = indexed.get(id);
            if (outfit != null) {
                ordered.add(outfit);
            }
        }
        return PageResult.of(favorites.getTotal(), favorites.getSize(), favorites.getCurrent(),
                toVOList(ordered, userId, false));
    }

    /**
     * 后台收藏记录分页。
     *
     * @param keyword 模糊词
     * @param pageNo  页码
     * @param size    每页条数
     * @return 分页结果
     */
    public PageResult<Map<String, Object>> adminFavoritePage(String keyword, int pageNo, int size) {
        int offset = Math.max(0, (pageNo - 1) * size);
        List<Map<String, Object>> records = favoriteMapper.selectAdminPage(
                StringUtils.hasText(keyword) ? keyword.trim() : null, offset, size);
        long total = favoriteMapper.countAdminPage(
                StringUtils.hasText(keyword) ? keyword.trim() : null);
        return PageResult.of(total, size, pageNo, records);
    }

    // ------------------------------------------------------------------ 写入

    /**
     * 新增搭配（小程序与后台共用）。
     *
     * @param form 表单
     * @return 新建后的视图对象
     */
    @Transactional(rollbackFor = Exception.class)
    public OutfitVO create(OutfitForm form) {
        LoginUser user = UserContext.require();
        validate(form);

        Outfit outfit = new Outfit();
        BeanUtils.copyProperties(form, outfit);
        outfit.setId(nextId());
        outfit.setAuthorId(user.getUserId());
        outfit.setAuthorName(user.displayName());
        outfit.setAuthorAvatar(user.getAvatar());
        outfit.setItemCount(form.getItemIds().size());
        outfit.setLikeCount(0);
        outfit.setFavCount(0);
        outfit.setViewCount(0);
        outfit.setIsTop(0);
        if (outfit.getStatus() == null) {
            outfit.setStatus(1);
        }
        applyCover(outfit, form.getItemIds());
        applyTone(outfit, form);

        outfitMapper.insert(outfit);
        saveItems(outfit.getId(), form.getItemIds());
        outfitMapper.refreshItemCount(outfit.getId());
        log.info("[搭配] 新建 {} {}（{} 件）", outfit.getId(), outfit.getTitle(), form.getItemIds().size());
        return detail(outfit.getId(), user.getUserId(), false);
    }

    /**
     * 修改搭配（后台）。
     *
     * @param form 表单
     * @return 修改后的视图对象
     */
    @Transactional(rollbackFor = Exception.class)
    public OutfitVO update(OutfitForm form) {
        Outfit exists = requireOutfit(form.getId());
        validate(form);

        Outfit outfit = new Outfit();
        BeanUtils.copyProperties(form, outfit);
        outfit.setItemCount(form.getItemIds().size());
        if (!StringUtils.hasText(outfit.getAuthorId())) {
            outfit.setAuthorId(exists.getAuthorId());
        }
        applyCover(outfit, form.getItemIds());

        outfitMapper.updateById(outfit);
        saveItems(outfit.getId(), form.getItemIds());
        outfitMapper.refreshItemCount(outfit.getId());
        log.info("[搭配] 修改 {} {}", outfit.getId(), outfit.getTitle());
        return detail(outfit.getId(), null, false);
    }

    /**
     * 审核 / 上下架。
     *
     * @param id     搭配ID
     * @param status 0 待审核 1 已发布 2 已下架
     */
    public void audit(String id, Integer status) {
        if (status == null || status < 0 || status > 2) {
            throw new BizException("状态值非法");
        }
        requireOutfit(id);
        Outfit patch = new Outfit();
        patch.setId(id);
        patch.setStatus(status);
        outfitMapper.updateById(patch);
    }

    /**
     * 删除单条。
     *
     * @param id 搭配ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        requireOutfit(id);
        outfitItemMapper.deleteByOutfitId(id);
        favoriteMapper.delete(new QueryWrapper<OutfitFavorite>().eq("outfit_id", id));
        outfitMapper.deleteById(id);
        log.info("[搭配] 删除 {}", id);
    }

    /**
     * 批量删除。
     *
     * @param ids 逗号分隔ID
     * @return 实际删除条数
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(String ids) {
        List<String> list = splitIds(ids);
        if (list.isEmpty()) {
            throw new BizException("请选择要删除的搭配");
        }
        for (String id : list) {
            outfitItemMapper.deleteByOutfitId(id);
            favoriteMapper.delete(new QueryWrapper<OutfitFavorite>().eq("outfit_id", id));
        }
        int affected = outfitMapper.deleteBatchIds(list);
        log.info("[搭配] 批量删除 {} 条", affected);
        return affected;
    }

    /**
     * 点赞（幂等性由前端控制，此处只做原子自增）。
     *
     * @param id 搭配ID
     * @return 最新点赞数
     */
    public int like(String id) {
        Outfit outfit = requireOutfit(id);
        outfitMapper.incrementLikeCount(id);
        return (outfit.getLikeCount() == null ? 0 : outfit.getLikeCount()) + 1;
    }

    /**
     * 收藏 / 取消收藏。
     *
     * @param userId   用户ID
     * @param outfitId 搭配ID
     * @return {favorited, favCount}
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleFavorite(String userId, String outfitId) {
        Outfit outfit = requireOutfit(outfitId);
        int current = (outfit.getFavCount() == null ? 0 : outfit.getFavCount());
        boolean favorited;
        if (favoriteMapper.countByUserAndOutfit(userId, outfitId) > 0) {
            favoriteMapper.deleteByUserAndOutfit(userId, outfitId);
            outfitMapper.decrementFavCount(outfitId);
            favorited = false;
            current = Math.max(0, current - 1);
        } else {
            try {
                favoriteMapper.insert(new OutfitFavorite(userId, outfitId));
                outfitMapper.incrementFavCount(outfitId);
                favorited = true;
                current = current + 1;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                favorited = true;
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("favorited", favorited);
        result.put("favCount", current);
        return result;
    }

    /**
     * 删除某条收藏记录（后台）。
     *
     * @param favoriteId 收藏记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFavorite(Long favoriteId) {
        OutfitFavorite favorite = favoriteMapper.selectById(favoriteId);
        if (favorite == null) {
            throw BizException.notFound("收藏记录不存在");
        }
        favoriteMapper.deleteById(favoriteId);
        outfitMapper.decrementFavCount(favorite.getOutfitId());
    }

    // ------------------------------------------------------------------ 转换

    /**
     * 实体列表 → 视图对象列表（批量补齐 favorited，避免 N+1）。
     *
     * @param outfits    实体列表
     * @param userId     当前用户ID，可空
     * @param withItems  是否填充组成时装明细
     * @return 视图对象列表
     */
    public List<OutfitVO> toVOList(List<Outfit> outfits, String userId, boolean withItems) {
        List<OutfitVO> list = new ArrayList<OutfitVO>();
        if (outfits == null || outfits.isEmpty()) {
            return list;
        }
        Set<String> favoritedIds = new HashSet<String>();
        if (StringUtils.hasText(userId)) {
            favoritedIds.addAll(favoriteMapper.selectOutfitIdsByUser(userId));
        }
        for (Outfit outfit : outfits) {
            OutfitVO vo = new OutfitVO();
            BeanUtils.copyProperties(outfit, vo);
            vo.setStyleText(dictService.text(DictService.STYLE, outfit.getStyleTag()));
            vo.setColorText(dictService.text(DictService.COLOR, outfit.getColorTone()));
            vo.setColorValue(dictService.color(DictService.COLOR, outfit.getColorTone()));
            vo.setStatusText(statusText(outfit.getStatus()));
            if (StringUtils.hasText(userId)) {
                vo.setFavorited(favoritedIds.contains(outfit.getId()));
            }
            List<String> itemIds = outfitItemMapper.selectItemIds(outfit.getId());
            vo.setItemIds(itemIds);
            if (withItems) {
                vo.setItems(fashionItemService.byIds(itemIds));
            }
            list.add(vo);
        }
        return list;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "已发布";
            case 2:
                return "已下架";
            case 0:
                return "待审核";
            default:
                return "未知";
        }
    }

    // ------------------------------------------------------------------ 工具

    /**
     * 取搭配，不存在抛业务异常。
     *
     * @param id 主键
     * @return 实体
     */
    public Outfit requireOutfit(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BizException("缺少搭配ID");
        }
        Outfit outfit = outfitMapper.selectById(id);
        if (outfit == null) {
            throw BizException.notFound("搭配不存在或已删除");
        }
        return outfit;
    }

    /** 重建关联表。 */
    private void saveItems(String outfitId, List<String> itemIds) {
        outfitItemMapper.deleteByOutfitId(outfitId);
        int sort = 0;
        Set<String> seen = new LinkedHashSet<String>();
        for (String itemId : itemIds) {
            if (!StringUtils.hasText(itemId) || !seen.add(itemId)) {
                continue;
            }
            outfitItemMapper.insert(new OutfitFashionItem(outfitId, itemId.trim(), sort++));
        }
    }

    /** 封面缺省取第一件时装的图。 */
    private void applyCover(Outfit outfit, List<String> itemIds) {
        if (StringUtils.hasText(outfit.getCoverImage())) {
            return;
        }
        List<FashionItemVO> items = fashionItemService.byIds(itemIds);
        if (!items.isEmpty()) {
            outfit.setCoverImage(items.get(0).getImageUrl());
        }
    }

    /** 色调缺省取第一件时装的色系。 */
    private void applyTone(Outfit outfit, OutfitForm form) {
        if (StringUtils.hasText(form.getColorTone())) {
            return;
        }
        List<FashionItemVO> items = fashionItemService.byIds(form.getItemIds());
        if (!items.isEmpty()) {
            outfit.setColorTone(items.get(0).getColorFamily());
        }
    }

    private void validate(OutfitForm form) {
        if (form == null) {
            throw new BizException("提交内容为空");
        }
        if (!StringUtils.hasText(form.getTitle())) {
            throw new BizException("请填写搭配标题");
        }
        if (form.getTitle().trim().length() > 40) {
            throw new BizException("标题不超过 40 个字");
        }
        List<String> ids = form.getItemIds();
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请至少选择一件时装");
        }
        if (new LinkedHashSet<String>(ids).size() > MAX_ITEMS) {
            throw new BizException("一套搭配最多 " + MAX_ITEMS + " 件时装");
        }
    }

    private String nextId() {
        Integer max = outfitMapper.selectMaxSeq();
        int next = max == null ? 2001 : max + 1;
        return "of" + next;
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

    /**
     * 某个用户收藏的搭配ID集合（供其它服务复用）。
     *
     * @param userId 用户ID
     * @return 不可变集合
     */
    public Collection<String> favoritedIds(String userId) {
        if (!StringUtils.hasText(userId)) {
            return Collections.emptyList();
        }
        return favoriteMapper.selectOutfitIdsByUser(userId);
    }
}
