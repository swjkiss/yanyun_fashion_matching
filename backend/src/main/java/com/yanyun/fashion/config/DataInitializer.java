package com.yanyun.fashion.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

/**
 * 数据初始化器：应用启动时按顺序执行 db/schema.sql、db/data.sql。
 *
 * <p>两个脚本均为幂等脚本（CREATE TABLE IF NOT EXISTS / INSERT IGNORE），
 * 因此「删除数据库后重启」可自动重建结构与种子数据，保证 {@code java -jar} 一次启动即可用。</p>
 *
 * <p>实现 {@link SmartInitializingSingleton}，在 Web 容器开始接收请求之前完成初始化，
 * 避免启动瞬间请求打到空库。</p>
 *
 * @author yanyun backend
 */
@Slf4j
@Component
public class DataInitializer implements SmartInitializingSingleton {

    private static final String SCHEMA_SCRIPT = "db/schema.sql";

    private static final String DATA_SCRIPT = "db/data.sql";

    private final DataSource dataSource;

    private final boolean enabled;

    public DataInitializer(DataSource dataSource, @Value("${app.init.enabled:true}") boolean enabled) {
        this.dataSource = dataSource;
        this.enabled = enabled;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!enabled) {
            log.warn("[数据初始化] app.init.enabled=false，跳过建表与种子数据");
            return;
        }
        long start = System.currentTimeMillis();
        try (Connection connection = dataSource.getConnection()) {
            runScript(connection, SCHEMA_SCRIPT);
            runScript(connection, DATA_SCRIPT);
        } catch (Exception e) {
            log.error("[数据初始化] 失败：{}", e.getMessage(), e);
            throw new IllegalStateException("数据库初始化失败，请检查 MySQL 连接与 db/*.sql 脚本：" + e.getMessage(), e);
        }
        log.info("[数据初始化] 完成（schema.sql + data.sql 幂等执行），耗时 {} ms",
                System.currentTimeMillis() - start);
    }

    private void runScript(Connection connection, String classPath) {
        ClassPathResource resource = new ClassPathResource(classPath);
        if (!resource.exists()) {
            throw new IllegalStateException("缺少 SQL 脚本：" + classPath);
        }
        ScriptUtils.executeSqlScript(connection, new EncodedResource(resource, StandardCharsets.UTF_8));
        log.info("[数据初始化] 已执行 {}", classPath);
    }
}
