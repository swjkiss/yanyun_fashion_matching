package com.yanyun.fashion.controller;

import com.yanyun.fashion.common.BizException;
import com.yanyun.fashion.common.PageResult;
import com.yanyun.fashion.common.Result;
import com.yanyun.fashion.service.OutfitService;
import com.yanyun.fashion.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 后台·收藏记录与统计（docs/contract-api.md 第 5 节）。
 *
 * @author yanyun backend
 */
@RestController
@RequestMapping("/fashion")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    private final OutfitService outfitService;

    /**
     * 收藏记录分页。
     *
     * @param keyword  标题 / 用户昵称模糊词
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping("/favorite/list")
    public Result<PageResult<Map<String, Object>>> favoriteList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        int size = Math.max(1, Math.min(pageSize == null ? 10 : pageSize, 200));
        int current = pageNo == null || pageNo < 1 ? 1 : pageNo;
        return Result.ok(outfitService.adminFavoritePage(keyword, current, size));
    }

    /**
     * 删除收藏记录。
     *
     * @param id 收藏记录ID
     * @return true
     */
    @DeleteMapping("/favorite/delete")
    public Result<Boolean> deleteFavorite(@RequestParam("id") Long id) {
        if (id == null) {
            throw new BizException("缺少收藏记录ID");
        }
        outfitService.deleteFavorite(id);
        return Result.ok("删除成功", true);
    }

    /**
     * 概览统计。
     *
     * @return 统计结构
     */
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statsService.overview());
    }

    /**
     * 近 N 日趋势。
     *
     * @param days 天数，默认 7
     * @return 趋势数组
     */
    @GetMapping("/stats/trend")
    public Result<List<Map<String, Object>>> trend(
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        return Result.ok(statsService.trend(days == null ? 7 : days));
    }
}
