package com.yanyun.fashion.controller;

import com.yanyun.fashion.common.PageResult;
import com.yanyun.fashion.common.Result;
import com.yanyun.fashion.dto.FashionQuery;
import com.yanyun.fashion.dto.OutfitForm;
import com.yanyun.fashion.dto.OutfitQuery;
import com.yanyun.fashion.entity.FashionItem;
import com.yanyun.fashion.entity.Outfit;
import com.yanyun.fashion.security.LoginUser;
import com.yanyun.fashion.security.UserContext;
import com.yanyun.fashion.service.AuthService;
import com.yanyun.fashion.service.FashionItemService;
import com.yanyun.fashion.service.OutfitService;
import com.yanyun.fashion.vo.FashionItemVO;
import com.yanyun.fashion.vo.LoginVO;
import com.yanyun.fashion.vo.OutfitVO;
import com.yanyun.fashion.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序接口（docs/contract-api.md 第 6 节）。
 *
 * <p>与后台共用同一套 Service，差异在于：强制只看上架内容、按业务语义排序、
 * 每次详情访问累加浏览量、以及所有接口都带当前用户的收藏态。</p>
 *
 * @author yanyun backend
 */
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppController {

    /** 首页推荐条数。 */
    private static final int RECOMMEND_LIMIT = 6;

    /** 首页热门搭配条数。 */
    private static final int HOT_OUTFIT_LIMIT = 4;

    private final AuthService authService;

    private final FashionItemService fashionItemService;

    private final OutfitService outfitService;

    /**
     * 免密登录（演示环境）：固定签发 demo 演示账号令牌。
     *
     * <p>安全约束：不接受客户端传入的用户名。本接口匿名可达，
     * 若按用户名直通签发令牌，任何人可冒充 sys_user 表中的任意账号（含 admin 角色）。</p>
     *
     * @return 令牌与用户信息
     */
    @PostMapping("/guest-login")
    public Result<LoginVO> guestLogin() {
        return Result.ok("登录成功", authService.guestLogin());
    }

    /**
     * 首页聚合数据：一次请求拿全，减少小程序首屏请求数。
     *
     * @return 首页结构
     */
    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        Map<String, Object> banner = new LinkedHashMap<String, Object>();
        banner.put("image", "/images/hero-banner.jpg");
        banner.put("title", "云想衣裳");
        banner.put("subtitle", "十六声 · 时装图鉴");
        result.put("banner", banner);

        Map<String, Object> filters = fashionItemService.filters();
        List<?> categories = (List<?>) filters.get("categories");
        long itemTotal = 0L;
        for (Object row : categories) {
            Object count = ((Map<?, ?>) row).get("count");
            if (count instanceof Number) {
                itemTotal += ((Number) count).longValue();
            }
        }
        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("itemTotal", itemTotal);
        stats.put("outfitTotal", outfitService.page(new OutfitQuery(), true, null).getTotal());
        result.put("stats", stats);
        result.put("categories", categories);
        result.put("recommend", fashionItemService.recommend(RECOMMEND_LIMIT));

        OutfitQuery hot = new OutfitQuery();
        hot.setSort("hot");
        hot.setPageNo(1);
        hot.setPageSize(HOT_OUTFIT_LIMIT);
        result.put("hotOutfits", outfitService.page(hot, true, currentUserId()).getRecords());
        return Result.ok(result);
    }

    /**
     * 时装检索。
     *
     * @param query 检索条件
     * @return 分页结果
     */
    @GetMapping("/fashion/items")
    public Result<PageResult<FashionItemVO>> items(FashionQuery query) {
        return Result.ok(fashionItemService.page(query, true));
    }

    /**
     * 时装详情（浏览量 +1，附带相关时装与相关搭配）。
     *
     * @param id 时装ID
     * @return {item, related, outfits}
     */
    @GetMapping("/fashion/item/{id}")
    public Result<Map<String, Object>> itemDetail(@PathVariable("id") String id) {
        FashionItemVO item = fashionItemService.detailAndCount(id);
        FashionItem entity = fashionItemService.requireItem(id);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("item", item);
        result.put("related", fashionItemService.related(entity, 4));
        result.put("outfits", outfitsContaining(id, currentUserId()));
        return Result.ok(result);
    }

    /**
     * 筛选面板选项（含各维度计数）。
     *
     * @return 选项结构
     */
    @GetMapping("/fashion/filters")
    public Result<Map<String, Object>> filters() {
        return Result.ok(fashionItemService.filters());
    }

    /**
     * 搭配广场。
     *
     * @param query 检索条件
     * @return 分页结果
     */
    @GetMapping("/outfits")
    public Result<PageResult<OutfitVO>> outfits(OutfitQuery query) {
        return Result.ok(outfitService.page(query, true, currentUserId()));
    }

    /**
     * 搭配详情（浏览量 +1）。
     *
     * @param id 搭配ID
     * @return {outfit, items, favorited, author}
     */
    @GetMapping("/outfit/{id}")
    public Result<Map<String, Object>> outfitDetail(@PathVariable("id") String id) {
        String userId = currentUserId();
        OutfitVO outfit = outfitService.detail(id, userId, true);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("outfit", outfit);
        result.put("items", outfit.getItemIds());
        result.put("itemDetails", outfit.getItems());
        result.put("favorited", Boolean.TRUE.equals(outfit.getFavorited()));

        Map<String, Object> author = new LinkedHashMap<String, Object>();
        author.put("id", outfit.getAuthorId());
        author.put("nickname", outfit.getAuthorName());
        author.put("avatar", outfit.getAuthorAvatar());
        result.put("author", author);
        return Result.ok(result);
    }

    /**
     * 创建搭配。
     *
     * @param form 表单
     * @return 新建后的搭配
     */
    @PostMapping("/outfit")
    public Result<OutfitVO> createOutfit(@RequestBody OutfitForm form) {
        OutfitVO vo = outfitService.create(form);
        return Result.ok("搭配已发表", vo);
    }

    /**
     * 收藏 / 取消收藏。
     *
     * @param body {outfitId}
     * @return {favorited, favCount}
     */
    @PostMapping("/favorite/toggle")
    public Result<Map<String, Object>> toggleFavorite(@RequestBody Map<String, Object> body) {
        String outfitId = body == null || body.get("outfitId") == null
                ? null : String.valueOf(body.get("outfitId"));
        LoginUser user = UserContext.require();
        Map<String, Object> result = outfitService.toggleFavorite(user.getUserId(), outfitId);
        return Result.ok(Boolean.TRUE.equals(result.get("favorited")) ? "已收藏" : "已取消收藏", result);
    }

    /**
     * 我的收藏。
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping("/favorites")
    public Result<PageResult<OutfitVO>> favorites(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        LoginUser user = UserContext.require();
        return Result.ok(outfitService.myFavorites(user.getUserId(),
                pageNo == null || pageNo < 1 ? 1 : pageNo,
                Math.max(1, Math.min(pageSize == null ? 10 : pageSize, 100))));
    }

    /**
     * 我的搭配。
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping("/my/outfits")
    public Result<PageResult<OutfitVO>> myOutfits(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        LoginUser user = UserContext.require();
        return Result.ok(outfitService.myOutfits(user.getUserId(),
                pageNo == null || pageNo < 1 ? 1 : pageNo,
                Math.max(1, Math.min(pageSize == null ? 10 : pageSize, 100))));
    }

    /**
     * 点赞。
     *
     * @param id 搭配ID
     * @return {likeCount}
     */
    @PostMapping("/outfit/{id}/like")
    public Result<Map<String, Object>> like(@PathVariable("id") String id) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("likeCount", outfitService.like(id));
        return Result.ok(result);
    }

    /**
     * 我的资料与数据。
     *
     * @return {userInfo, myOutfitCount, favoriteCount, likeSum}
     */
    @GetMapping("/user/profile")
    public Result<Map<String, Object>> profile() {
        LoginUser user = UserContext.require();
        UserInfoVO info = authService.userInfo(user.getUserId());

        OutfitQuery mine = new OutfitQuery();
        mine.setPageNo(1);
        mine.setPageSize(1);
        long myOutfitCount = outfitService.page(mine, true, null).getTotal();

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("userInfo", info);
        result.put("myOutfitCount", myOutfitCount);
        result.put("favoriteCount", outfitService.favoritedIds(user.getUserId()).size());
        result.put("likeSum", sumLike(user.getUserId()));
        return Result.ok(result);
    }

    // ------------------------------------------------------------------ 内部

    private String currentUserId() {
        LoginUser user = UserContext.get();
        return user == null ? null : user.getUserId();
    }

    /** 包含某时装的搭配（最多 6 套）。 */
    private List<OutfitVO> outfitsContaining(String itemId, String userId) {
        List<String> outfitIds = new ArrayList<String>();
        OutfitQuery query = new OutfitQuery();
        query.setPageNo(1);
        query.setPageSize(60);
        PageResult<OutfitVO> page = outfitService.page(query, true, userId);
        for (OutfitVO vo : page.getRecords()) {
            if (vo.getItemIds() != null && vo.getItemIds().contains(itemId)) {
                outfitIds.add(vo.getId());
            }
            if (outfitIds.size() >= 6) {
                break;
            }
        }
        List<OutfitVO> list = new ArrayList<OutfitVO>();
        for (String id : outfitIds) {
            list.add(outfitService.detail(id, userId, false));
        }
        return list;
    }

    /** 我的搭配累计获赞。 */
    private long sumLike(String userId) {
        long sum = 0L;
        OutfitQuery query = new OutfitQuery();
        query.setPageNo(1);
        query.setPageSize(200);
        for (Outfit outfit : outfitService.page(query, true, null).getRecords()) {
            if (userId != null && userId.equals(outfit.getAuthorId()) && outfit.getLikeCount() != null) {
                sum += outfit.getLikeCount();
            }
        }
        return sum;
    }
}
