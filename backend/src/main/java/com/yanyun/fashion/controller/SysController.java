package com.yanyun.fashion.controller;

import com.yanyun.fashion.common.BizException;
import com.yanyun.fashion.common.Result;
import com.yanyun.fashion.security.LoginUser;
import com.yanyun.fashion.security.UserContext;
import com.yanyun.fashion.service.AuthService;
import com.yanyun.fashion.service.DictService;
import com.yanyun.fashion.storage.StorageService;
import com.yanyun.fashion.vo.LoginVO;
import com.yanyun.fashion.vo.UploadVO;
import com.yanyun.fashion.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统接口：登录、当前用户、退出、数据字典、文件上传。
 *
 * <p>路径与 JeecgBoot 保持一致，便于前端直接套用其请求封装。</p>
 *
 * @author yanyun backend
 */
@Slf4j
@RestController
@RequestMapping("/sys")
@RequiredArgsConstructor
public class SysController {

    private final AuthService authService;

    private final DictService dictService;

    private final StorageService storageService;

    /**
     * 后台登录。
     *
     * @param body {username, password}
     * @return 令牌与用户信息
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody Map<String, Object> body) {
        String username = body == null ? null : String.valueOf(body.get("username"));
        String password = body == null ? null : String.valueOf(body.get("password"));
        return Result.ok("登录成功", authService.login(username, password));
    }

    /**
     * 当前登录用户。
     *
     * @return 用户信息与角色
     */
    @GetMapping("/user/getUserInfo")
    public Result<Map<String, Object>> getUserInfo() {
        LoginUser user = UserContext.require();
        UserInfoVO info = authService.userInfo(user.getUserId());
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("userInfo", info);
        result.put("roles", Arrays.asList(user.getRole() == null ? "user" : user.getRole()));
        return Result.ok(result);
    }

    /**
     * 退出登录（JWT 无状态，服务端仅作语义返回）。
     *
     * @return true
     */
    @PostMapping("/logout")
    public Result<Boolean> logout() {
        return Result.ok(true);
    }

    /**
     * 按字典编码取字典项（JeecgBoot 原生形态）。
     *
     * @param dictCode 字典编码，如 fashion_rarity
     * @return [{text, value, title, color}]
     */
    @GetMapping("/dict/getDictItems/{dictCode}")
    public Result<List<Map<String, Object>>> getDictItems(@PathVariable("dictCode") String dictCode) {
        List<Map<String, Object>> items = dictService.toDictItems(dictCode);
        if (items.isEmpty()) {
            throw BizException.notFound("字典不存在：" + dictCode);
        }
        return Result.ok(items);
    }

    /**
     * 字典列表。
     *
     * @return [{id, dictCode, dictName, itemCount}]
     */
    @GetMapping("/dict/list")
    public Result<List<Map<String, Object>>> dictList() {
        return Result.ok(dictService.listDicts());
    }

    /**
     * 图片上传。
     *
     * @param file 文件
     * @param biz  业务子目录（保留字段，当前统一落 uploads/）
     * @return 访问地址与原始文件名
     */
    @PostMapping("/common/upload")
    public Result<UploadVO> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "biz", required = false) String biz) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        log.info("[上传] 收到文件 {} ({}/{} bytes)", file.getOriginalFilename(), file.getSize(),
                StringUtils.hasText(biz) ? biz : "-");
        return Result.ok("上传成功", storageService.saveImage(file));
    }
}
