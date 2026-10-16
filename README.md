# 碎银记账本 · Java 后端（Spring Boot）

适合学习的技术组合：

- **Java 17**
- **Spring Boot**（Web + JDBC）
- **MySQL**（建表/种子脚本在本项目 `sql/` 目录）
- **JdbcTemplate**：SQL 写得清楚，方便对照建表脚本学习

## 目录怎么读（学习路线）

```
sql/          ← 建库建表与种子数据
controller/   ← 接收 HTTP 请求（你最先看这里）
repository/   ← 用 SQL 访问数据库
dto/          ← 返回给前端的数据结构
config/       ← 跨域等配置
```

请求链路：`Controller → Repository(JdbcTemplate) → MySQL`

## 准备数据库

在项目根目录执行：

```powershell
cd d:\my_project\suiyin-api
mysql -u root -p < sql\schema.sql
mysql -u root -p suiyin_ledger -e "SOURCE d:/my_project/suiyin-api/sql/seed_bootstrap.sql; SOURCE d:/my_project/suiyin-api/sql/seed_categories.sql;"
```

或按顺序手动执行：

1. `sql/schema.sql`
2. `sql/seed_bootstrap.sql`
3. `sql/seed_categories.sql`

## 配置密码

PowerShell：

```powershell
$env:MYSQL_PASSWORD="你的MySQL密码"
```

或改 `src/main/resources/application.yml` 里的：

```yaml
spring:
  datasource:
    password: 你的密码
```

（不要把真实密码提交到 Git）

## 启动

本机未装 Maven 也没关系，用自带 Wrapper：

```powershell
cd d:\my_project\suiyin-api
.\mvnw.cmd -s .mvn\settings.xml spring-boot:run
```

（已配置阿里云 Maven 镜像，国内下载更快）

第一次会下载依赖，稍等一会儿。

启动成功后访问：

- http://127.0.0.1:3000/health
- http://127.0.0.1:3000/health/db
- http://127.0.0.1:3000/api/bootstrap?openid=demo_openid_suiyin
- http://127.0.0.1:3000/api/transactions?ledgerId=1&yearMonth=2026-10

## 接口一览

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/health` | 服务存活 |
| GET | `/health/db` | 数据库连通 |
| GET | `/api/bootstrap` | 用户/账本/账户/分类 |
| GET | `/api/transactions` | 流水列表 |

## 已对接小程序

`suiyin-mini` 已请求本服务。请同时启动后端与 H5，并保证 `VITE_API_BASE` 指向本机端口。

## 下一步可学

1. 加微信登录（code 换 openid）
2. 加 JWT / 登录态校验
3. 分页、预算等接口
