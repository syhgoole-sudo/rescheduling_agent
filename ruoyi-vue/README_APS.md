# 面向插单扰动的车间动态重调度智能决策系统

## 1. 项目简介

本项目是一个面向离散制造车间插单扰动场景的 APS 动态重调度原型系统。系统围绕“初始调度方案 + 高优先级插单 + 影响分析 + 局部重调度 + KPI/甘特图对比 + 人工确认”的业务闭环展开，帮助调度员理解插单对原计划的影响，并基于量化指标和可视化结果判断是否采用新的重调度方案。

当前项目定位为个人项目 / MVP 原型系统，不是生产上线系统。

## 2. 技术栈

- 后端：若依 Spring Boot
- 前端：若依 Vue
- 数据库：MySQL
- 缓存：Redis
- 算法服务：Python FastAPI
- 可视化：ECharts
- 算法：规则调度算法、局部重调度算法
- 解释模块：规则模板式解释报告

## 3. 系统架构

系统采用前后端分离 + 独立算法服务架构：

- Vue 前端负责页面交互、基础 CRUD、甘特图和对比结果展示。
- Java 后端负责业务编排、权限控制、数据库读写、方案版本管理和调用 Python 服务。
- MySQL 保存产品、工艺路线、设备、订单、调度方案、任务明细和插单事件。
- Redis 支撑若依登录会话和缓存能力。
- Python FastAPI 负责初始调度和局部重调度计算。

Java 后端不直接写复杂排产算法，Python 算法服务不直接访问 MySQL。两者通过 HTTP JSON 接口解耦。

## 4. 核心业务流程

1. 维护产品、工艺路线、设备组和设备。
2. 维护 NORMAL 普通订单和 INSERT 插单订单。
3. 生成 INITIAL 初始调度方案。
4. 保存调度方案主表和任务明细。
5. 查看任务明细和单方案甘特图。
6. 创建插单事件并分析影响范围。
7. 推荐局部重调度策略。
8. 调用 Python 局部重调度接口生成 RESCHEDULE 新方案。
9. 保存新方案和完整任务集合。
10. 查看新旧方案 KPI 对比。
11. 查看新旧甘特图对比。
12. 生成规则模板式解释报告。
13. 调度员采用或拒绝重调度方案。

## 5. 已实现功能

- APS 基础数据 CRUD。
- 普通订单初始调度。
- 调度方案版本保存。
- 调度任务明细保存与查询。
- 单方案甘特图展示。
- 插单事件创建。
- 插单影响范围分析。
- 局部重调度策略推荐。
- 局部重调度新方案生成。
- 新旧方案 KPI 对比。
- 新旧甘特图对比。
- 重调度方案采用 / 拒绝。
- 规则模板式解释报告。

## 6. 启动方式

### 启动 MySQL

未设置环境变量时使用本地开发默认值：

- database：`ry-vue`
- username：`root`
- password：`1234`
- port：`3306`

数据库、Druid 监控账号和 JWT 密钥均支持通过环境变量覆盖：

| 环境变量 | 本地默认值 | 用途 |
| --- | --- | --- |
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/ry-vue?...` | MySQL JDBC URL |
| `MYSQL_USERNAME` | `root` | MySQL 用户名 |
| `MYSQL_PASSWORD` | `1234` | MySQL 密码 |
| `MYSQL_SLAVE_ENABLED` | `false` | 是否启用从库 |
| `MYSQL_SLAVE_URL` | 空 | 从库 JDBC URL |
| `MYSQL_SLAVE_USERNAME` | 空 | 从库用户名 |
| `MYSQL_SLAVE_PASSWORD` | 空 | 从库密码 |
| `DRUID_USERNAME` | `ruoyi` | Druid 监控用户名 |
| `DRUID_PASSWORD` | `123456` | Druid 监控密码 |
| `JWT_SECRET` | `abcdefghijklmnopqrstuvwxyz` | JWT 签名密钥 |

本地环境可以不设置任何变量直接启动。部署或推送公开仓库时，应在操作系统、容器编排或密钥管理服务中提供实际值，不要把真实密码、Token 或 API Key 写入 YAML。PowerShell 示例：

```powershell
$env:MYSQL_URL='jdbc:mysql://localhost:3306/ry-vue?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8'
$env:MYSQL_USERNAME='root'
$env:MYSQL_PASSWORD='replace-with-local-password'
$env:DRUID_USERNAME='replace-with-monitor-user'
$env:DRUID_PASSWORD='replace-with-monitor-password'
$env:JWT_SECRET='replace-with-a-long-random-secret'
```

配置模板见 `ruoyi-admin/src/main/resources/application-example.yml`。模板中的默认值仅用于本地开发，生产环境必须覆盖密码和 JWT 密钥。

Windows 服务方式示例：

```powershell
net start MySQL80
```

### 启动 Redis

```powershell
docker run -d --name local-redis -p 6379:6379 redis:7
```

如果容器已存在：

```powershell
docker start local-redis
```

### 启动 Python FastAPI 算法服务

```powershell
cd C:\Users\admin\Desktop\rescheduling_agent\ruoyi-vue\aps-algorithm-service
python -m uvicorn app.main:app --reload --port 8000
```

### 启动若依后端

```powershell
cd C:\Users\admin\Desktop\rescheduling_agent\ruoyi-vue
mvn -pl ruoyi-admin spring-boot:run
```

### 启动若依前端

```powershell
cd C:\Users\admin\Desktop\rescheduling_agent\ruoyi-vue\ruoyi-ui
npm run dev
```

## 7. 演示流程

推荐演示顺序：

1. 登录若依系统。
2. 查看产品管理、订单管理、工艺路线工序、设备组和设备数据。
3. 在调度方案页面生成初始调度。
4. 查看初始方案任务明细。
5. 查看单方案甘特图。
6. 在插单事件页面创建并分析插单。
7. 查看影响分析结果。
8. 点击推荐策略。
9. 生成局部重调度方案。
10. 返回调度方案页面查看 RESCHEDULE 新方案。
11. 查看 KPI 对比。
12. 查看新旧甘特图对比。
13. 在插单事件页面查看解释报告。
14. 在调度方案页面采用或拒绝新方案。

更详细的演示脚本见：

- `docs/aps_demo_script.md`

## 8. 项目亮点

- 业务闭环完整，覆盖插单扰动下的动态重调度全过程。
- 架构分层清晰，Java 负责业务闭环，Python 负责算法计算。
- 支持 INITIAL 与 RESCHEDULE 方案追溯。
- 支持任务明细、单方案甘特图和新旧甘特图对比。
- 支持 KPI 量化对比，辅助调度员决策。
- 解释报告明确 AI/Agent 边界，只解释方案，不直接排产。

## 9. 后续扩展方向

- 设备故障、交期提前、加工时间延迟等更多扰动类型。
- 多扰动叠加处理。
- 多算法策略池，例如遗传算法、禁忌搜索、强化学习调度。
- MES / ERP 数据集成。
- 设备日历、班次、维护窗口和工装约束。
- 更完整的方案确认记录和版本树。
- 外部大模型接入，用于自然语言解释和问答，但仍不直接生成排产结果。

## 10. AI / Agent 边界

本项目中的解释模块不直接生成任务时间、不分配设备、不改变调度结果。排产结果必须由规则算法或优化算法计算。AI / Agent 的合理定位是解释影响范围、策略选择、KPI 差异和方案取舍，辅助调度员决策。
