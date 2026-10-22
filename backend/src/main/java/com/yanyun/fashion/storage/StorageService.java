package com.yanyun.fashion.storage;

import com.yanyun.fashion.common.BizException;
import com.yanyun.fashion.vo.UploadVO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 图片存储服务。
 *
 * <p>目录约定（相对 backend/ 运行目录）：</p>
 * <pre>
 * upload/images/              ← /images/** 静态映射根目录
 *   ├─ hero-banner.jpg
 *   ├─ fashion/               ← 时装主图（由插画脚本产出，本服务只读）
 *   ├─ outfit/                ← 搭配封面
 *   ├─ avatar/                ← 用户头像
 *   └─ uploads/               ← 后台上传落盘目录
 * </pre>
 *
 * <p>启动时自动创建目录；目录缺失或创建失败不会导致启动失败（图片可能稍后由其它环节产出）。</p>
 *
 * @author yanyun backend
 */
@Slf4j
@Component
public class StorageService {

    /** 静态资源子目录。 */
    private static final String[] SUB_DIRS = new String[]{"fashion", "outfit", "avatar", "uploads"};

    private final String urlPrefix;

    private final long maxSize;

    private final Set<String> allowedExtensions;

    /** 静态图片根目录（/images/** 映射目标）。 */
    @Getter
    private final Path baseDir;

    /** 上传落盘目录。 */
    @Getter
    private final Path uploadDir;

    public StorageService(@Value("${app.upload.base-dir:upload/images}") String baseDir,
                          @Value("${app.upload.url-prefix:/images}") String urlPrefix,
                          @Value("${app.upload.max-size:5242880}") long maxSize,
                          @Value("${app.upload.allowed-ext:jpg,jpeg,png,webp}") String allowedExt) {
        this.baseDir = resolveBaseDir(baseDir);
        this.uploadDir = this.baseDir.resolve("uploads");
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
        this.maxSize = maxSize;
        this.allowedExtensions = new LinkedHashSet<String>();
        for (String ext : Arrays.asList(allowedExt.split(","))) {
            if (!ext.trim().isEmpty()) {
                this.allowedExtensions.add(ext.trim().toLowerCase(Locale.ROOT));
            }
        }
        ensureDirectories();
    }

    /**
     * 解析图片根目录：优先当前工作目录，其次兼容从仓库根目录启动。
     *
     * @param configured 配置值，可为相对或绝对路径
     * @return 绝对路径
     */
    private Path resolveBaseDir(String configured) {
        Path configuredPath = Paths.get(configured);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        Path cwd = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path direct = cwd.resolve(configured).normalize();
        if (Files.isDirectory(direct)) {
            return direct;
        }
        Path fromRepoRoot = cwd.resolve("backend").resolve(configured).normalize();
        if (Files.isDirectory(fromRepoRoot)) {
            return fromRepoRoot;
        }
        if (Files.isDirectory(cwd.resolve("backend"))) {
            return fromRepoRoot;
        }
        return direct;
    }

    /** 幂等创建目录，失败只告警，不影响启动。 */
    private void ensureDirectories() {
        createDirectory(baseDir);
        for (String subDir : SUB_DIRS) {
            createDirectory(baseDir.resolve(subDir));
        }
        log.info("[静态资源] /images/** -> {}", baseDir);
    }

    private void createDirectory(Path dir) {
        try {
            if (!Files.isDirectory(dir)) {
                Files.createDirectories(dir);
                log.info("[静态资源] 已创建目录 {}", dir);
            }
        } catch (IOException e) {
            log.warn("[静态资源] 目录创建失败 {}：{}", dir, e.getMessage());
        }
    }

    /**
     * 静态资源映射地址（以 / 结尾，符合 Spring ResourceHandler 约定）。
     *
     * @return file: URI 字符串
     */
    public String resourceLocation() {
        String uri = baseDir.toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }

    /**
     * 保存上传图片。
     *
     * @param file 上传文件
     * @return 访问 url 与原始文件名
     */
    public UploadVO saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String extension = extensionOf(originalName);
        if (!allowedExtensions.contains(extension)) {
            throw new BizException("仅支持 jpg / jpeg / png / webp 格式的图片");
        }
        if (file.getSize() > maxSize) {
            throw new BizException("文件过大，单个文件不得超过 5MB");
        }
        String storedName = System.currentTimeMillis() + "-" + randomToken() + "." + extension;
        try {
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(storedName).normalize();
            if (!target.startsWith(uploadDir)) {
                throw new BizException("非法的文件路径");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("[上传] {} ({} bytes) -> {}", originalName, file.getSize(), target);
        } catch (IOException e) {
            log.error("[上传] 保存失败 {}", originalName, e);
            throw new BizException("文件保存失败，请稍后重试");
        }
        UploadVO vo = new UploadVO();
        vo.setUrl(urlPrefix + "/uploads/" + storedName);
        vo.setFileName(originalName);
        return vo;
    }

    private String extensionOf(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String randomToken() {
        long value = ThreadLocalRandom.current().nextLong(0x10000000L, 0xFFFFFFFFL);
        return Long.toHexString(value);
    }
}
