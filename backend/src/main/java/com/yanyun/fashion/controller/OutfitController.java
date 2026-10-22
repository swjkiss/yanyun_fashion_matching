package com.yanyun.fashion.controller;

import com.yanyun.fashion.common.BizException;
import com.yanyun.fashion.common.PageResult;
import com.yanyun.fashion.common.Result;
import com.yanyun.fashion.dto.OutfitForm;
import com.yanyun.fashion.dto.OutfitQuery;
import com.yanyun.fashion.service.OutfitService;
import com.yanyun.fashion.vo.OutfitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台·搭配管理（docs/contract-api.md 第 4 节）。
 *
 * @author yanyun backend
 */
@RestController
@RequestMapping("/fashion/outfit")
@RequiredArgsConstructor
public class OutfitController {

    private final OutfitService outfitService;

    /**
     * 分页查询。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<PageResult<OutfitVO>> list(OutfitQuery query) {
        return Result.ok(outfitService.page(query, false, null));
    }

    /**
     * 详情（含组成时装明细）。
     *
     * @param id 搭配ID
     * @return 视图对象
     */
    @GetMapping("/queryById")
    public Result<OutfitVO> queryById(@RequestParam("id") String id) {
        return Result.ok(outfitService.detail(id, null, false));
    }

    /**
     * 新增。
     *
     * @param form 表单
     * @return 新建后的视图对象
     */
    @PostMapping("/add")
    public Result<OutfitVO> add(@RequestBody OutfitForm form) {
        return Result.ok("新增成功", outfitService.create(form));
    }

    /**
     * 修改。
     *
     * @param form 表单
     * @return 修改后的视图对象
     */
    @PutMapping("/edit")
    public Result<OutfitVO> edit(@RequestBody OutfitForm form) {
        return Result.ok("修改成功", outfitService.update(form));
    }

    /**
     * 删除。
     *
     * @param id 搭配ID
     * @return true
     */
    @DeleteMapping("/delete")
    public Result<Boolean> delete(@RequestParam("id") String id) {
        outfitService.delete(id);
        return Result.ok("删除成功", true);
    }

    /**
     * 批量删除。
     *
     * @param ids 逗号分隔ID
     * @return 实际删除条数
     */
    @DeleteMapping("/deleteBatch")
    public Result<Integer> deleteBatch(@RequestParam("ids") String ids) {
        return Result.ok("批量删除成功", outfitService.deleteBatch(ids));
    }

    /**
     * 审核 / 上下架。
     *
     * @param body {id, status}
     * @return true
     */
    @PutMapping("/audit")
    public Result<Boolean> audit(@RequestBody Map<String, Object> body) {
        String id = body == null ? null : String.valueOf(body.get("id"));
        Object status = body == null ? null : body.get("status");
        if (id == null || "null".equals(id) || status == null) {
            throw new BizException("缺少参数 id 或 status");
        }
        outfitService.audit(id, Integer.valueOf(String.valueOf(status)));
        return Result.ok("状态已更新", true);
    }
}
