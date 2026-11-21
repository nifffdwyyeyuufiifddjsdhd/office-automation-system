# 部署说明（OA 办公系统 v2.0）

面向单机 / 小规模内网部署：Spring Boot 打成一个 jar + 前端静态文件交给 Nginx。

## 一、准备

| 组件 | 版本要求 | 说明 |
| --- | --- | --- |
| JDK | 17 | 后端编译与运行 |
| MySQL | 8.0+ | 字符集 utf8mb4 |
| Node.js | 20+ | 只用于构建前端产物 |
| Nginx | 1.20+ | 托管前端并反代 /api |

## 二、初始化数据库

```powershell
# 建库 + 导入基线（59 张表 + 演示数据），库已存在会询问是否重建
.\scripts\init-db.ps1 -Password 你的密码
# 追加强制重建
.\scripts\init-db.ps1 -Password 你的密码 -Database oa_system -Force
```

生产环境建议单独建库账号，而不是直接用 root：

```sql
CREATE USER 'oa_app'@'%' IDENTIFIED BY '强密码';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON oa_system.* TO 'oa_app'@'%';
FLUSH PRIVILEGES;
```

## 三、构建

```powershell
# 后端 jar
cd OfficeAutomationSystemAPI
.\mvnw.cmd -o -DskipTests package
# 前端静态文件 → OfficeAutomationSystemWeb\dist
cd ..\OfficeAutomationSystemWeb
npm ci
npm run build
```

## 四、配置环境变量

```powershell
Copy-Item .\deploy\env.prod.example .\deploy\.env.prod
notepad .\deploy\.env.prod
```

必填项：`OA_DB_URL`、`OA_DB_USERNAME`、`OA_DB_PASSWORD`、`OA_JWT_SECRET`、`OA_UPLOAD_DIR`、`OA_MAIL_*`。
prod 配置文件里这些项没有默认值，漏填会直接启动失败（这是故意的，避免生产用默认密钥）。

## 五、启动

```powershell
# Windows：脚本会加载 .env.prod 再 java -jar
.\deploy\start-backend.ps1 -Rebuild

# Linux：等价命令
set -a && . ./deploy/.env.prod && set +a
java -Xmx2g -jar OfficeAutomationSystemAPI/target/*.jar
```

Nginx：把 `deploy/nginx.conf` 放进 conf.d，改 `server_name` 和 `root` 指向 `dist` 目录，然后 `nginx -s reload`。
前端同域部署时不需要配 `OA_CORS_ORIGINS`；前后端分域才需要填前端域名。

## 六、上线后自检

```powershell
# 免登录接口，返回 code=200 说明后端活着
curl http://127.0.0.1:4040/api/public/announcements
# 用 admin 登录换个 token，确认鉴权链路
curl -X POST http://127.0.0.1:4040/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"123456"}'
```

浏览器打开站点 → 用 admin 登录 → 首页应有待办/公告 → 系统管理里能看到系统参数。

## 七、忘记管理员密码

基线数据里的密码是 bcrypt 密文，反推不出来。忘了就用脚本直接重置（现场签管理员 token 调接口，不需要先登录）：

```powershell
.\scripts\reset-password.ps1 -Username admin -NewPassword 'Oa@2026#Init'
```

脚本依赖 `OA_JWT_SECRET`（不传则用环境变量或代码默认值）。**重置完记得把密钥换掉**，能签 token 就能当管理员。

## 八、运维要点

- **日志**：prod 默认写 `OA_LOG_FILE`，按天滚动保留 30 天；异常日志还会落库（系统管理 → 异常日志）。
- **备份**：每天备份数据库（`mysqldump --single-transaction`）与 `OA_UPLOAD_DIR` 目录，二者要一起回滚。
- **限流**：`config/RateLimitInterceptor` 是进程内滑动窗口（登录 20 次/分、其它 600 次/分）。多实例部署时计数不共享，需要换成 Redis 实现。
- **上传**：白名单 + 魔数校验在应用层做，Nginx 只兜 `client_max_body_size`，两边都要保留。
- **升级**：新增的 `sql-migration-*.sql` 按文件名日期顺序执行；执行前先备份。
