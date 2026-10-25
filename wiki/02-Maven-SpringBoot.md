# 02 Maven 与 Spring Boot 启动：后端服务到底怎么跑起来

## 这一页解决什么

前端 `pnpm dev` 起来的是一个开发服务器；后端 `mvn spring-boot:run` 起来的是一个**常驻 JVM 进程**。
这一页讲清：依赖谁管、配置从哪来、启动类做了什么、怎么验证服务活着。

## Maven：后端版依赖 + 构建系统

三个 POM：

| 文件 | 作用 |
| --- | --- |
| `backend/pom.xml` | 父工程：Java 17、Spring Boot 版本、模块关系（`contract` + `service`）、统一依赖版本 |
| `backend/contract/pom.xml` | 契约模块：只放 Request / Response / enum / Facade 接口，不是可运行服务 |
| `backend/service/pom.xml` | 服务模块：Web、Validation、Security、Redis、MyBatis-Plus、MySQL、JWT、springdoc、测试依赖 |

看依赖就像前端看 `package.json`：看到 `spring-boot-starter-web` 就知道能写 Controller，
看到 `mybatis-plus` 就知道用 ORM 操作数据库。

## Spring Boot：把 Java 服务跑起来

启动类：`backend/service/src/main/java/com/example/fullstackmall/service/FullstackMallApplication.java`

```java
@SpringBootApplication
public class FullstackMallApplication {
    public static void main(String[] args) {
        SpringApplication.run(FullstackMallApplication.class, args);
    }
}
```

`@SpringBootApplication` 做了三件事：组件扫描、自动配置、配置加载。
项目里的 `@RestController`、`@Service`、`@Configuration` 都会被扫描并交给 Spring 容器管理。

为什么用 `@Resource` 注入而不是自己 `new`？
因为后端对象可能涉及事务代理、安全代理、生命周期和配置注入，交给容器统一管理更可靠。

## 配置文件

- `backend/service/src/main/resources/application.yml`
- `application-local.yml`、`application-debug.yml`（本地 / 调试 profile）

典型写法（环境变量可覆盖默认值）：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3308/fullstack_mall?...}
    username: ${DB_USERNAME:mall}
    password: ${DB_PASSWORD:mall_dev_password}
server:
  port: ${SERVER_PORT:8080}
security:
  jwt:
    secret: ${JWT_SECRET}     # 公开仓库不内置密钥，启动前必须自己注入
```

## 动手

```bash
# 1) 起依赖
docker compose -f docker-compose.dev.yml up -d mysql redis

# 2) 编译 + 跑测试 + 打包
cd backend && mvn clean package

# 3) 起服务（注意 -pl service -am 会自动带上 contract 模块）
mvn -pl service -am spring-boot:run

# 4) 或者跑 jar（需要 JWT_SECRET）
export JWT_SECRET="$(openssl rand -base64 32)"
java -jar service/target/fullstack-mall-service-0.0.1-SNAPSHOT.jar
```

验证服务活着：

```bash
curl -sS http://localhost:8080/api/health
```

启动后可用：

- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>

## 常见坑

- **没起 MySQL / Redis**：后端不是纯内存应用，缺依赖会出现连接失败或功能降级
- **没设 `JWT_SECRET`**：配置里是 `${JWT_SECRET}`，没有默认值，启动会直接失败
- **端口冲突**：MySQL 容器映射到 3308 而不是 3306，就是为了避免和你本机已有实例冲突
- **只 build 了 service 没 build contract**：用 `mvn -pl service -am` 或直接在 `backend/` 下整体构建

## 自查题

- 启动类和 `ProductController` 是什么关系？
- `application.yml` 和前端的 `.env` 有什么相似和不同？
- 为什么后端常用环境变量注入密钥，而不是写死在配置文件里？
