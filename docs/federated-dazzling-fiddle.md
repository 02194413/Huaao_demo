# 短链接生成器 — 开发计划

## Context

基于现有 `huaao_demo` Spring Boot 项目（`com.lzp` 包，Spring Boot 3.5.16 + Java 17），扩展为短链接生成器 REST API。目标是交付一个可落地使用的短链服务，包含核心功能、Docker 部署、单元测试、简易前端和 API 文档。

## 技术栈

| 组件 | 版本/方案 |
|------|----------|
| Spring Boot | 3.5.16（已有） |
| MyBatis-Plus | 3.5.x |
| MySQL | 8.0 |
| Redis | 7.x（缓存加速跳转） |
| Docker | Dockerfile + docker-compose |
| 前端 | 单页 HTML（放在 static/ 下） |
| API 文档 | springdoc-openapi |
| 测试 | JUnit 5 + Mockito |

## 项目结构（目标）

```
huaao_demo/
├── src/main/java/com/lzp/
│   ├── HuaaoDemoApplication.java
│   ├── config/
│   │   ├── RedisConfig.java
│   │   └── SwaggerConfig.java         # OpenAPI 配置
│   ├── controller/
│   │   └── UrlController.java         # REST 接口
│   ├── service/
│   │   ├── UrlService.java            # 业务接口
│   │   └── impl/
│   │       └── UrlServiceImpl.java    # 业务实现
│   ├── mapper/
│   │   └── UrlMappingMapper.java      # MyBatis-Plus Mapper
│   ├── entity/
│   │   └── UrlMapping.java            # 数据库实体
│   ├── dto/
│   │   ├── CreateShortUrlRequest.java
│   │   └── ShortUrlResponse.java
│   ├── common/
│   │   ├── Result.java                # 统一响应体
│   │   └── GlobalExceptionHandler.java
│   └── util/
│       └── Base62Util.java            # Base62 编解码
├── src/main/resources/
│   ├── application.yml                # 主配置（替代 properties）
│   ├── static/
│   │   └── index.html                 # 简易前端
│   └── db/
│       └── init.sql                   # 建表脚本
├── src/test/java/com/lzp/
│   ├── util/
│   │   └── Base62UtilTest.java
│   ├── service/
│   │   └── UrlServiceImplTest.java
│   └── controller/
│       └── UrlControllerTest.java
├── sql/
│   └── init.sql
├── Dockerfile
├── docker-compose.yml
├── pom.xml                            # 补充依赖
└── README.md                          # 项目说明
```

## 分阶段执行计划

### 阶段 1：项目重构 + 核心功能

**1.1 补充 pom.xml 依赖**（1 轮）
- 添加 MyBatis-Plus、MySQL 驱动、Redis、springdoc-openapi、H2（测试用）
- 调整 Spring Boot 版本到实际可用的 3.x

**1.2 配置 application.yml**（1 轮）
- 数据源配置（MySQL）
- Redis 配置
- MyBatis-Plus 配置
- 服务器端口

**1.3 创建数据库初始化脚本**（1 轮）
- sql/init.sql：`url_mapping` 表，字段：id, short_code, original_url, visit_count, last_visit_at, expire_at, created_at

**1.4 核心代码开发**（3-4 轮）
- Base62Util：编码/解码工具类
- UrlMapping Entity + UrlMappingMapper
- DTO：CreateShortUrlRequest, ShortUrlResponse
- UrlService + UrlServiceImpl：生成短链、查询、跳转、访问统计
- UrlController：POST /api/shorten, GET /{shortCode}, GET /api/info/{shortCode}
- Result + GlobalExceptionHandler

**交付检查：** 项目可启动，三个核心接口可用

### 阶段 2：单元测试

**2.1 编写测试**（2-3 轮）
- Base62UtilTest：编码/解码/边界值
- UrlServiceImplTest：Mock Mapper/Redis，覆盖正常+异常场景
- UrlControllerTest：MockMvc 测试 HTTP 层
- 运行 `mvn test` 确保全部通过

### 阶段 3：Docker 部署 + 前端 + API 文档

**3.1 Docker 化**（2 轮）
- Dockerfile：多阶段构建
- docker-compose.yml：MySQL + Redis + App
- 配置 Docker 环境参数

**3.2 简易前端**（1-2 轮）
- index.html：输入长 URL → 生成短链 → 展示结果 + 跳转测试 + 统计查看
- 简洁 CSS 美化

**3.3 API 文档**（1 轮）
- SwaggerConfig + Controller 注解
- 验证 /swagger-ui.html 可访问

### 阶段 4：收尾 + README

**4.1 代码审查与优化**（1 轮）
- 边界处理：无效 URL、短码不存在、过期短链、重复长 URL
- 日志配置

**4.2 README.md**（1 轮）
- 选题说明、技术栈、项目结构、启动方式、API 接口说明

---

## 核心设计决策

### 短链生成算法
自增 ID → Base62 编码 → 6 位短码。简单可靠，无需引入额外 ID 生成服务。

### 缓存策略
- 生成短链时：写入 Redis（key=shortCode, value=originalUrl, TTL=24h）
- 跳转时：先查 Redis，未命中查 DB，查到时回写 Redis
- 访问计数：异步更新 DB，避免阻塞跳转

### 包结构
沿用现有 `com.lzp` 根包，短链功能在子包 `controller`、`service`、`entity` 等下展开。

---

## 验证方式

1. `mvn test` — 单元测试全部通过
2. `mvn spring-boot:run` — 启动后用 curl/浏览器测试三个接口
3. `docker-compose up --build` — Docker 一键启动后访问
4. 访问 `http://localhost:8080/swagger-ui.html` — API 文档可查看
5. 访问 `http://localhost:8080/index.html` — 前端页面可正常使用
