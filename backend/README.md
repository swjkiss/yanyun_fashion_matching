# 燕云十六声 · 后端服务（Spring Boot）

古风时装图鉴 + 搭配圈后端，遵循 JeecgBoot 规范：统一响应体 `Result<T>`、`X-Access-Token` 鉴权头、`/sys/login` 登录、MD5(用户名+密码+盐) 口令散列。

## 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| Java | 1.8（JDK 1.8.0_251 已验证） | 无模块化特性依赖 |
| Spring Boot | 2.7.18 | Web + Validation |
| MyBatis-Plus | 3.5.5 | ORM，含分页插件 |
| MySQL | 8.0 | `yanyun_fashion` 库，root/root |
| java-jwt | 3.19.4 | 登录令牌（7 天有效） |
| Lombok | — | 编译期生成 getter/setter |

## 快速开始

```bash
# 1. 启动 MySQL（Windows 服务）
net start MySQL80

# 2. 构建并运行（仓库根目录执行）
mvn -f backend/pom.xml package -DskipTests
"D:\Program Files\Java\jdk1.8.0_251\bin\java.exe" -Dfile.encoding=UTF-8 -jar backend/target/yanyun-fashion-server.jar
```

- 服务地址：`http://127.0.0.1:8080/jeecg-boot`
- 首次启动自动建库建表并灌入样例数据（16 时装 / 8 搭配 / 3 用户）
- 删库重启即可自愈重灌：`DROP DATABASE yanyun_fashion;`

## 速查

- 管理端账号：`admin / admin123`
- 游客登录：`POST /app/guest-login`
- 接口契约见 `docs/contract-api.md`
- 静态图片：`http://127.0.0.1:8080/jeecg-boot/images/**` ← `backend/upload/images/**`

## 目录

```
backend/
├─ src/main/java/com/yanyun/fashion/
│  ├─ controller/    # Sys / Fashion / Outfit / App 控制器
│  ├─ service/       # 业务逻辑
│  ├─ mapper/        # MyBatis-Plus mapper
│  ├─ entity/ vo/    # 数据模型
│  ├─ security/      # JWT 鉴权拦截器
│  ├─ storage/       # 图片上传与静态资源
│  └─ common/        # Result / BizException / 全局异常
└─ src/main/resources/db/   # schema.sql + data.sql（幂等）
```
