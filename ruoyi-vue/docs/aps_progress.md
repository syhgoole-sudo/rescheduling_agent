# APS MVP 开发进度说明

## 1. 当前已完成的功能

- 若依前后端分离版项目搭建完成。
- MySQL 数据库 `ry-vue` 初始化完成。
- Redis 已启动并可被若依后端访问。
- APS MVP 8 张核心表已创建：
  - `aps_product`
  - `aps_route_operation`
  - `aps_equipment_group`
  - `aps_equipment`
  - `aps_order`
  - `aps_schedule_plan`
  - `aps_schedule_task`
  - `aps_insert_event`
- APS 基础 CRUD 已生成并接入若依菜单。
- 初始调度 Python FastAPI 服务已创建。
- Java 后端已能调用 Python 初始调度接口。
- 初始调度方案已能保存到 `aps_schedule_plan`。
- 调度任务明细已能保存到 `aps_schedule_task`。
- 调度任务页面已支持按 `plan_id` 查看任务。
- 甘特图页面已能展示设备维度任务条。
- 插单事件已能创建。
- 插单影响范围分析已跑通。
- `impact_json` 已能保存插单影响分析结果，并在前端展示。

## 2. 当前可以运行的服务

- MySQL：
  - 数据库：`ry-vue`
  - 用户名：`root`
  - 密码：`1234`
  - 地址：`localhost:3306`
- Redis：
  - Docker 镜像：`redis:7`
  - 地址：`localhost:6379`
- 若依后端：
  - 模块：`ruoyi-admin`
  - 默认端口：`8080`
- 若依前端：
  - 模块：`ruoyi-ui`
  - 默认端口：`80` 或本地 dev server 输出端口
- Python 算法服务：
  - 目录：`aps-algorithm-service`
  - 端口：`8000`

## 3. 常用启动命令

启动 Redis：

```bash
docker start local-redis
```

如果容器不存在，可重新创建：

```bash
docker run -d --name local-redis -p 6379:6379 redis:7
```

启动 Python 算法服务：

```bash
cd C:\Users\admin\Desktop\rescheduling_agent\ruoyi-vue\aps-algorithm-service
python -m uvicorn app.main:app --reload --port 8000
```

启动若依后端：

```bash
cd C:\Users\admin\Desktop\rescheduling_agent\ruoyi-vue
mvn -pl ruoyi-admin spring-boot:run
```

如果模块依赖异常，先执行：

```bash
cd C:\Users\admin\Desktop\rescheduling_agent\ruoyi-vue
mvn clean install -DskipTests
```

启动若依前端：

```bash
cd C:\Users\admin\Desktop\rescheduling_agent\ruoyi-vue\ruoyi-ui
npm run dev
```

如 PowerShell 拦截 `npm.ps1`，可使用：

```bash
npm.cmd run dev
```

## 4. 当前数据库中的关键测试数据

- 插单订单 ID 当前为 `1009`。
- 插单订单编码为 `O-INSERT-001`。
- 当前已有 `ACTIVE` 初始调度方案。
- 当前已有插单影响分析结果。
- `aps_insert_event.impact_json` 已保存影响范围分析 JSON。

## 5. 已知注意事项

- 插单订单 ID 不能固定写死为 `9`，当前环境中为 `1009`。
- 后续应把手输插单订单 ID 改成下拉选择，避免测试数据 ID 变化导致误操作。
- 菜单路由新增或调整后，需要重新登录前端才能刷新动态路由。
- 修改 Java 后必须重启若依后端。
- 修改前端页面或 API 后，需要重新运行 `npm run dev`。
- Python 算法服务必须先启动，否则初始调度接口会调用失败。
- 若依后端依赖 Redis，启动后端前应确认 Redis 可连接。

## 6. 下一步待开发功能

- 局部重调度策略推荐。
- 冻结任务 / 可调整任务 / 插单任务划分。
- Python 局部重调度接口。
- Java 调用局部重调度接口。
- 保存 `RESCHEDULE` 新方案。
- 新旧方案 KPI 对比。
- 新旧甘特图对比。
- AI Agent 解释报告。

## 7. 下一步建议

下一阶段建议优先做“局部重调度策略推荐”，先基于 `aps_insert_event.impact_json` 完成策略判断和任务范围划分。

暂时不要直接生成新方案。先把冻结任务、可调整任务、插单任务的边界定义清楚，再进入 Python 局部重调度接口开发。
