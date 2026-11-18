# 校园文具商城

面向校园文具选购的 Web 应用。买家可以搜索商品、加入购物车、提交订单，并在重新登录后查看自己的订单。管理员可以维护商品和库存，查看订单并处理订单状态。

## 功能

| 买家端 | 管理端 |
| --- | --- |
| 注册、登录与退出 | 管理员后台访问校验 |
| 按名称搜索上架商品 | 商品维护、上下架与库存管理 |
| 修改购物车数量并结算 | 查看订单、完成或取消订单 |
| 查看本人订单的状态与明细 | 取消订单后回补库存 |

## 技术栈

Java 25 · Spring Boot 4.1.2 · Spring MVC · Thymeleaf · Spring Data JPA · Spring Security · MySQL · Maven Wrapper

## 本地运行

1. 安装 JDK 25 和 MySQL 8，启动 MySQL。项目默认连接 `localhost:3306`。
2. 在 MySQL 中创建开发数据库，并为应用账号授予该库的读写和建表权限。

   ```sql
   CREATE DATABASE IF NOT EXISTS stationery_shop CHARACTER SET utf8mb4;
   ```

3. 在项目根目录运行以下 PowerShell 命令。将用户名替换为你的本地 MySQL 账号。首次启动时，Hibernate 会创建或更新表结构。

   ```powershell
   $env:SPRING_DATASOURCE_USERNAME = 'stationery_app'
   $env:DB_PASSWORD = Read-Host '请输入 MySQL 密码' -MaskInput
   .\mvnw.cmd spring-boot:run
   ```

4. 打开 [商城首页](http://localhost:8080/shop)。首次使用空库时，可注册一个演示账号，再在本地数据库将该账号的 `user_account.role` 更新为 `ADMIN`，重新登录后在后台添加并上架商品。另注册一个普通账号演示购买流程。

## 订单与库存

- 下单时通过带库存和上架条件的数据库更新扣减库存。扣减、保存订单和清空购物车在同一事务内执行。
- 订单明细保存下单时的商品名称、单价和数量。订单列表与详情按登录账号查询，其他账号的订单详情返回 404。
- 管理员处理订单时使用数据库行锁，取消订单的状态变更与库存回补在同一事务内执行。

## 验证

运行 `.\mvnw.cmd test` 执行自动化回归测试。测试使用内存 H2，无需 MySQL 凭据，覆盖管理员删除权限与 CSRF、买家下单及订单归属、并发扣减、库存回滚和并发取消。

演示流程可以从普通账号开始，搜索商品并下单，退出后重新登录，在「我的订单」打开订单详情。管理员取消该订单后，买家可以刷新详情查看状态变化。
