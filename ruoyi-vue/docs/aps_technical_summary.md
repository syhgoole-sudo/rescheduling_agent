# APS MVP 技术实现总结

## 1. 数据库设计

MVP 使用 8 张 APS 核心表：

- `aps_product`：产品表。
- `aps_route_operation`：产品工艺路线工序表。
- `aps_equipment_group`：设备组表。
- `aps_equipment`：设备表。
- `aps_order`：订单表，普通订单和插单订单共用。
- `aps_schedule_plan`：调度方案主表，区分 INITIAL 和 RESCHEDULE。
- `aps_schedule_task`：调度任务明细表，用于任务查询和甘特图展示。
- `aps_insert_event`：插单事件表，保存影响分析、策略和新旧方案绑定关系。

设计特点：

- 不使用物理外键，符合若依常见业务开发方式。
- 调度方案通过 `source_plan_id` 追溯原方案。
- 插单事件通过 `source_plan_id` 和 `new_plan_id` 绑定原方案和新方案。
- `kpi_json` 保存方案 KPI。
- `impact_json` 保存影响分析和策略推荐结果。

## 2. Java 后端模块

APS 后端位于若依业务模块中，主要包含：

- Domain：实体对象。
- Mapper：MyBatis 数据访问接口。
- Mapper XML：SQL 映射。
- Service：业务接口。
- ServiceImpl：业务编排和状态流转。
- Controller：前端 REST 接口。
- Client/DTO：Java 调用 Python FastAPI 的请求和响应结构。

Java 后端职责：

- 查询基础数据和订单。
- 组装算法服务请求。
- 调用 Python FastAPI。
- 保存调度方案和任务明细。
- 维护方案状态和插单事件状态。
- 提供 KPI 对比、甘特图数据和解释报告接口。

## 3. Python 算法服务

Python 服务基于 FastAPI，独立于若依后端，主要接口：

- `POST /api/schedule/initial`：初始调度。
- `POST /api/schedule/reschedule/local`：局部重调度。

Python 服务职责：

- 接收 Java 后端传入的订单、工艺路线、设备资源和调度约束。
- 根据规则算法生成任务开始时间、结束时间和设备分配。
- 返回完整任务列表和 KPI。

Python 服务不直接访问 MySQL，避免算法服务与业务数据库强耦合。

## 4. Java 调用 Python 的接口方式

Java 后端通过 HTTP 调用 Python FastAPI。

调用流程：

1. Java 从 MySQL 查询订单、工艺路线、设备和已有方案任务。
2. Java 组装请求 DTO。
3. Java Client 调用 Python 接口。
4. Python 返回 JSON 响应。
5. Java 校验 `success` 字段。
6. Java 保存方案主表和任务明细。

这样可以保持 Java 负责业务闭环，Python 负责算法计算。

## 5. 初始调度算法逻辑

初始调度只处理 `order_type = NORMAL` 且 `order_status = NEW` 的订单。

排序规则：

1. `priority_level` 升序。
2. `due_time` 升序。
3. `release_time` 升序。

排程规则：

- 每个订单按 `process_seq` 顺序加工。
- 每道工序只能选择对应 `equipment_group_id` 下的设备。
- 同一订单后续工序不能早于前序工序结束。
- 同一设备同一时间不能加工两个任务。
- 第一工序开始时间不能早于订单释放时间。
- 设备选择最早可用设备。

输出：

- 调度任务列表。
- makespan、延期订单数、总延期分钟、最大延期分钟、任务数等 KPI。

## 6. 插单影响分析逻辑

影响分析基于当前 ACTIVE 的 INITIAL 方案。

分析步骤：

1. 查询插单订单。
2. 根据插单订单产品查询工艺路线。
3. 获取插单需要经过的设备组集合。
4. 从当前方案任务中筛选：
   - `planned_start_time >= 插单 release_time`
   - `equipment_group_id` 属于插单设备组集合
   - `is_frozen = N`
5. 得到受影响任务、订单和设备。
6. 根据受影响任务数判断影响等级。

影响等级：

- `LOW`：受影响任务数不超过 3。
- `MEDIUM`：4 到 8。
- `HIGH`：大于 8。

分析结果写入 `aps_insert_event.impact_json`。

## 7. 策略推荐逻辑

策略推荐基于 `impact_json` 中的影响等级和任务数量。

规则：

- `LOW`：推荐 `RIGHT_SHIFT`，即右移重调度。
- `MEDIUM`：推荐 `LOCAL_RESCHEDULE`，即局部重调度。
- `HIGH`：推荐 `LOCAL_RESCHEDULE_WITH_INSERT_PRIORITY`，即插单优先的局部重调度。

同时划分：

- 冻结任务：未受影响任务。
- 可调整任务：受影响任务。
- 插单任务：根据插单订单工艺路线展开的工序任务。

策略结果写入 `impact_json.strategy` 和 `strategy_type`。

## 8. 局部重调度逻辑

局部重调度输入：

- 插单订单。
- 冻结任务。
- 可调整任务。
- 插单任务。
- 设备资源。
- 工艺路线。
- 策略配置。

规则：

- 冻结任务保持原计划不变。
- 可调整任务和插单任务重新排程。
- 插单任务优先级最高。
- 订单内部仍按工序顺序加工。
- 设备选择最早可用设备。
- 同一设备不允许任务重叠。
- 返回完整方案任务集合，而不是只返回变化任务。

任务标识：

- 冻结任务：`is_frozen = Y`。
- 插单任务：`is_inserted = Y`。
- 被调整任务：`is_changed = Y`。
- 原受影响任务通过 `source_task_id` 追溯原任务。

## 9. KPI 对比计算逻辑

KPI 对比针对原方案和新方案计算：

- 原方案任务数。
- 新方案任务数。
- 原方案 makespan。
- 新方案 makespan。
- 原方案延期订单数。
- 新方案延期订单数。
- 原方案总延期分钟。
- 新方案总延期分钟。
- 原方案最大延期分钟。
- 新方案最大延期分钟。
- 插单订单完工时间。
- 插单订单延期分钟。
- 变更任务数。
- 变更任务比例。
- 插单任务数。
- 冻结任务数。

延期计算：

- 每个订单完工时间取该订单所有任务的最大 `planned_end_time`。
- 延期分钟为 `max(0, finish_time - due_time)`。

## 10. 甘特图实现

单方案甘特图：

- 后端接口按 `plan_id` 查询任务。
- 按设备分组返回任务集合。
- 前端使用 ECharts custom series 绘制横向任务条。
- 横轴为时间，纵轴为设备。

新旧甘特图对比：

- 后端根据 RESCHEDULE 方案找到 `source_plan_id`。
- 分别查询原方案和新方案任务。
- 前端上下展示两个甘特图。
- 新方案通过 `is_inserted`、`is_changed`、`is_frozen` 区分插单任务、变更任务和冻结任务。

## 11. 解释报告生成逻辑

解释报告接口：

- `GET /aps/insertEvent/explain/{eventId}`

输入数据：

- 插单事件。
- `impact_json`。
- 策略结果。
- 原方案。
- 新方案。
- KPI 对比结果。

生成方式：

- 第一版使用规则模板实时生成。
- 不写入数据库。
- 不调用外部大模型 API。

报告内容：

- 插单事件概述。
- 影响范围说明。
- 策略推荐说明。
- 重调度结果说明。
- KPI 对比说明。
- 方案收益。
- 方案代价。
- 调度建议。

## 12. AI Agent 边界说明

本项目中 AI/Agent 的定位是解释和辅助决策，不是排产引擎。

明确边界：

- 不直接生成最终排产结果。
- 不直接分配设备。
- 不直接决定任务开始和结束时间。
- 不绕过工艺路线、设备能力、订单交期和任务顺序约束。
- 不替代规则算法或优化算法。

AI/Agent 应做的事：

- 解释插单影响范围。
- 解释为什么推荐某种策略。
- 解释 KPI 变化。
- 总结方案收益和代价。
- 给出调度员可读的采用建议。

这种边界可以保证调度结果可追溯、可解释、可验证。
