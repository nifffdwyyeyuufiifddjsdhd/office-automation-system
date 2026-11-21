# OA 办公自动化系统

企业级 OA 系统，前后端分离：后端 Spring Boot 3 + MyBatis-Plus + MySQL，前端 Vue 3 + Vite + TypeScript + Element Plus。
按角色（管理员 / 总经理 / 财务 / 人事主管 / 人事专员 / 项目经理 / 部门主管 / 员工 / 审核人动态角色 / 访客）划分模块与数据范围。

## 目录结构

```
office-automation-system/
├─ OfficeAutomationSystemAPI/     后端（端口 4040，接口前缀 /api）
│  ├─ src/main/java/com/officeautomationsystemapi/
│  │  ├─ controller/   27 个控制器，302 个接口
│  │  ├─ service/      业务实现（数据范围、权限、脱敏、金额识别等）
│  │  ├─ repository/   MyBatis-Plus Mapper
│  │  ├─ entity/ vo/   实体与出参
│  │  ├─ config/       拦截器、限流、跨域、MyBatis-Plus 配置
│  │  └─ common/       统一响应 Result、权限注解、错误码、Excel 清洗
│  └─ src/main/resources/application.yml + application-dev.yml + application-prod.yml
├─ OfficeAutomationSystemWeb/     前端（Vite 端口 5174，hash 路由）
│  └─ src/{api,views,router,store,config,utils}
└─ （同级目录）oa-mini-program/  小程序端（uni-app + Vue 3 + TS，一套代码出 H5/微信小程序）
   └─ src/{api,pages,store,utils}
├─ docs/                          交付文档（接口对账、交付核对表、数据库说明、早期接口清单）
├─ deploy/                        部署材料（Nginx、生产配置模板、启动脚本）
├─ scripts/                       初始化与运维脚本（一键建库、密码重置、数据库一致性自检）
└─ sql-migration-*.sql            26 个增量迁移脚本
```

## 环境要求

| 组件 | 版本 |
| --- | --- |
| JDK | 17 |
| Maven | 3.9+（仓库自带 mvnw） |
| MySQL | 8.0 |
| Node.js | 18+ |
| 浏览器 | Chrome / Edge 最新版 |

## 快速启动

### 1. 初始化数据库

```powershell
# 建库 + 依次执行全部迁移脚本（需要 mysql 客户端在 PATH，或用 -MySqlExe 指定）
.\scripts\init-db.ps1 -User root -Password 861202
```

脚本会创建 `oa_system` 库、导入基础结构与 26 个增量迁移，并把内置账号补齐。

### 2. 启动后端

```powershell
cd OfficeAutomationSystemAPI
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
.\mvnw.cmd -o -DskipTests spring-boot:run
```

默认端口 4040，健康检查：`GET http://localhost:4040/api/public/announcements`。

### 3. 启动前端

```powershell
cd OfficeAutomationSystemWeb
npm install
npm run dev
```

打开 http://localhost:5174 ，默认代理到 `/api`（见 `.env.example`）。

### 3.1 小程序端（可选）

小程序端已独立成同级工程 `D:\VSC\Employment Project\oa-mini-program`（和本工程、医院管理系统平级），
不另建服务，直接复用本后端的 `/api`：

```powershell
cd ..\oa-mini-program
npm install
npm run dev:h5           # 浏览器里按手机尺寸调试
npm run dev:mp-weixin    # 产物 dist/dev/mp-weixin，用微信开发者工具导入
```

也可以直接用 HBuilderX 打开 `oa-mini-program` 目录运行。后端地址写在 `src/config.ts` 的 `BASE_URL`
（默认 `http://localhost:4040/api`）；真机或小程序上线要换成 https 域名，并在小程序后台配置
request/uploadFile 合法域名。详见 [oa-mini-program/README.md](../oa-mini-program/README.md)。

### 4. 演示账号

| 账号 | 角色 | 说明 |
| --- | --- | --- |
| admin | 系统管理员 | 全部功能 |
| manager01 | 部门主管 | 市场部，本部门及下级 |
| employee01 | 普通员工 | 技术部 |
| hr01 / hrmanager01 | 人事专员 / 人事主管 | 人事部 |
| finance01 | 财务专员 | 财务部 |
| gm01 | 总经理/高管 | 全公司只读 + 高级审批 |
| pm01 | 项目经理 | 技术部，项目数据 |
| guest | 访客 | 只有公开公告 |

演示账号的密码取决于建库时导入的数据（基线 dump 里是 bcrypt 密文，无法反推）。
如果不知道密码或想统一初始化，用运维脚本直接重置（走管理员接口，不需要先登录）：

```powershell
# 重置 admin
.\scripts\reset-password.ps1 -Username admin -NewPassword 'Oa@2026#Init'
# 批量：对其他演示账号重复执行即可
```

重置后登录会按 `must_change_password` 标记决定是否强制改密。

## 配置与环境变量

后端全部敏感配置走环境变量（`application.yml` 里给了本地默认值，生产用 `OA_PROFILE=prod` + `application-prod.yml` 强制必填）：

| 变量 | 说明 | 默认 |
| --- | --- | --- |
| `OA_PROFILE` | 运行环境 dev/test/prod | dev |
| `SERVER_PORT` | 后端端口 | 4040 |
| `OA_DB_URL` / `OA_DB_USERNAME` / `OA_DB_PASSWORD` | 数据库 | 本地 oa_system |
| `OA_JWT_SECRET` / `OA_JWT_EXPIRE_HOURS` | JWT 密钥与有效期 | 演示密钥 / 24 |
| `OA_UPLOAD_DIR` | 上传文件目录 | 项目下 uploads |
| `OA_CORS_ORIGINS` | 跨域来源白名单，逗号分隔 | localhost:5173/5174 |
| `OA_MAIL_HOST` / `OA_MAIL_USERNAME` / `OA_MAIL_PASSWORD` | 邮件（找回密码验证码） | 空（dev profile 给了本地演示值） |

前端变量见 `OfficeAutomationSystemWeb/.env.example`。

## 接口约定

- 统一响应：`{ "code": 200, "message": "操作成功", "data": ..., "timestamp": 1792833645747 }`
- 业务失败返回 HTTP 200 + 四位业务码（1001 账号与密码、2001 流程、3001 出勤、4001 任务）；未登录 401、无权限 403、限流 429。
- 登录换取 JWT，后续请求带 `Authorization: Bearer <token>`；前端把 token 存 sessionStorage。
- 限流：登录类每分钟 20 次、其它接口每分钟 600 次（按来源 IP 滑动窗口，单机内存实现）。
- 接口以 Controller 上的 `@RequestMapping` + 方法注解为准；想查「后端有哪些路径、前端有没有接上」，看 [docs/接口对账.md](docs/接口对账.md)。

## 可观测性与运维

| 能力 | 实现 | 在哪看 |
| --- | --- | --- |
| 操作审计 | `interceptor/AuditLogInterceptor` 拦截全部写操作（POST/PUT/PATCH/DELETE）落 `operation_logs`；业务层已手写日志的请求用 `AuditLogService` 标记去重，一次请求只记一条 | 系统管理 → 操作日志 |
| 异常日志 | `GlobalExceptionHandler` 自动记录未捕获异常（类型/消息/堆栈/URL/IP/操作人）；业务提示不落库，避免刷屏 | 系统管理 → 异常日志 |
| 报错措辞 | 数据库异常不把表名和 SQL 回给前端，统一「数据查询失败」；非法日期/数字按 400 提示 | 接口响应 |
| 通知模板 | `NotificationTemplateService` + `TemplateRenderer` 渲染 `notification_templates`，覆盖待办到达、审批结果、任务指派、缺勤提醒，模板停用自动回退兜底文案 | 系统管理 → 通知模板 / 推送记录 |
| 登录审计 | 成功失败都写 `login_logs`，含 IP、浏览器、失败原因 | 登录日志 |

## 测试与构建

```powershell
# 后端单测（Excel 注入防护 / JWT / 脱敏 / 限流 / 模板渲染 / 金额与类别识别）
cd OfficeAutomationSystemAPI
.\mvnw.cmd -o test

# 前端类型检查 + 打包（产物 dist/）
cd ../OfficeAutomationSystemWeb
npm run build

# 小程序端打包（独立工程在 ..\oa-mini-program；H5 产物 dist/build/h5，微信产物 dist/build/mp-weixin）
cd ..\..\oa-mini-program
npm run build:h5
npm run build:mp-weixin

# 数据库一致性自检（24 条跨表规则，只读；有违例退出码 1）
cd ..\office-automation-system
.\scripts\check-db-invariants.ps1 -Password 861202
```

## 部署

见 [deploy/README.md](deploy/README.md)：Nginx 反代 + 静态托管、生产环境变量、后端 jar 启动脚本。

## 文档

| 文档 | 内容 | 什么时候看 |
| --- | --- | --- |
| [docs/接口对账.md](docs/接口对账.md) | 后端 Controller 与前端 api 封装的路径比对：只后端有的 9 条（全是刻意直连的导出/图片/运维端点）、只前端有的 0 条 | 怀疑「接口写了没接上」或「前端调了不存在的路径」 |
| [docs/交付核对表.md](docs/交付核对表.md) | 需求文档逐条对照（已实现 / 替代方案 / 未做项） | 交付验收 |
| [docs/数据库说明.md](docs/数据库说明.md) | 核心表结构、字段含义、迁移脚本顺序 | 换环境初始化、加字段 |
| [docs/接口清单.md](docs/接口清单.md) | 早期扁平清单（271 条，只有方法+路径），保留作历史对照 | 快速扫一遍后端有哪些路径 |

接口对账是脚本生成的，改完 Controller 或 `src/api/*.ts` 后重跑即可保持一致（`scripts/gen-api-docs.py` 现在只作为对账脚本的解析库被复用，单独运行会重建两份已下线的接口文档）：

```powershell
python scripts\check-api-coverage.py   # 刷新 docs/接口对账.md
```
