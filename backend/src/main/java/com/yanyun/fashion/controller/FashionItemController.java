package com.yanyun.fashion.controller;

import com.yanyun.fashion.common.BizException;
import com.yanyun.fashion.common.PageResult;
import com.yanyun.fashion.common.Result;
import com.yanyun.fashion.dto.FashionQuery;
import com.yanyun.fashion.entity.FashionItem;
import com.yanyun.fashion.service.FashionItemService;
import com.yanyun.fashion.vo.FashionItemVO;
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
 * 后台·时装图鉴管理（docs/contract-api.md 第 3 节）。
 *
 * @author yanyun backend
 */
@RestController
@RequestMapping("/fashion/item")
@RequiredArgsConstructor
public class FashionItemController {

    private final FashionItemService fashionItemService;

    /**
     * 分页查询。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<PageResult<FashionItemVO>> list(FashionQuery query) {
        return Result.ok(fashionItemService.page(query, false));
    }

    /**
     * 详情。
     *
     * @param id 时装ID
     * @return 视图对象
     */
    @GetMapping("/queryById")
    public Result<FashionItemVO> queryById(@RequestParam("id") String id) {
        return Result.ok(fashionItemService.getById(id));
    }

    /**
     * 新增。
     *
     * @param form 表单
     * @return 新建后的视图对象
     */
    @PostMapping("/add")
    public Result<FashionItemVO> add(@RequestBody FashionItem form) {
        return Result.ok("新增成功", fashionItemService.create(form));
    }

    /**
     * 修改。
     *
     * @param form 表单
     * @return 修改后的视图对象
     */
    @PutMapping("/edit")
    public Result<FashionItemVO> edit(@RequestBody FashionItem form) {
        return Result.ok("修改成功", fashionItemService.update(form));
    }

    /**
     * 删除。
     *
     * @param id 时装ID
     * @return true
     */
    @DeleteMapping("/delete")
    public Result<Boolean> delete(@RequestParam("id") String id) {
        fashionItemService.delete(id);
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
        return Result.ok("批量删除成功", fashionItemService.deleteBatch(ids));
    }

    /**
     * 上架 / 下架。
     *
     * @param body {id, status}
     * @return true
     */
    @PutMapping("/status")
    public Result<Boolean> status(@RequestBody Map<String, Object> body) {
        String id = body == null ? null : String.valueOf(body.get("id"));
        Object status = body == null ? null : body.get("status");
        if (id == null || "null".equals(id) || status == null) {
            throw new BizException("缺少参数 id 或 status");
        }
        fashionItemService.updateStatus(id, Integer.valueOf(String.valueOf(status)));
        return Result.ok("状态已更新", true);
    }
}
