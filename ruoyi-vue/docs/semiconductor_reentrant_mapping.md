# 半导体可重入制造场景映射说明

## 1. 当前系统对象与半导体对象映射

| 当前 APS 对象 | 半导体制造对象 | 说明 |
| --- | --- | --- |
| `aps_order` | lot / wafer lot | MVP 阶段将订单抽象为调度对象，在半导体语境下可解释为晶圆批次 lot。 |
| `aps_product` | 产品类型 / 工艺族 | 表示某类产品或工艺族，不直接表达单片 wafer。 |
| `aps_route_operation` | 工艺路线 step | 表示产品对应的工艺步骤。 |
| `process_seq` | step 顺序 | 表示 lot 在工艺路线中的加工顺序。 |
| `equipment_group` | tool group / 设备族 | 表示具有相同或相近加工能力的一组机台。 |
| `equipment` | tool / 机台 | 表示具体可加工任务的设备。 |
| `schedule_task` | lot-step 加工任务 | 表示某个 lot 在某个 step 上分配到某台 tool 的加工任务。 |
| `insert_event` | 插单 lot 扰动事件 | 表示高优先级 lot 到达后对当前方案造成的扰动。 |

## 2. 当前系统如何表达可重入

当前系统没有单独建立半导体专用的 layer、recipe 或 lot-route 表，但可以通过 `aps_route_operation` 表达可重入路线：

- 同一个 `product_id` 的工艺路线中，同一 `equipment_group_id` 可以在不同 `process_seq` 上出现多次。
- 同一 lot 在不同 step 上多次进入同一设备族，即可解释为 reentrant flow。
- 甘特图展示的是 tool 维度的 lot-step 加工结果，可以观察同一设备族在不同时间段处理不同 lot-step 的负荷情况。

当前 demo 数据已经体现可重入特征：

- `P-B` 路线为：`CUT -> CNC -> HEAT -> CNC -> INSPECT`，其中 `CNC` 设备组出现两次，可用于模拟半导体 lot 多次回到同一 tool group。
- `P-C` 路线中 `INSPECT` 设备组出现两次，也可作为重复进入同类设备组的简化示例。

如果使用半导体术语描述，可将 `P-B` 理解为一个简化的 reentrant route。例如：

- 通用表达：`CNC -> HEAT -> CNC -> INSPECT`
- 半导体类比：`PHOTO -> ETCH/DIFF -> PHOTO -> INSPECT/CMP`

## 3. 当前系统如何表达插单

当前系统使用 `aps_order` 同时表达普通订单和插单订单。在半导体语境下：

- `order_type = INSERT` 可解释为插单 lot 或 hot lot。
- `priority_level` 可解释为 lot 等级、hot lot 等级或加急等级，数值越小优先级越高。
- `release_time` 可解释为 lot 到达时间、释放时间或可进入调度窗口的时间。
- `due_time` 可解释为 lot 交期或承诺完成时间。
- `order_status` 可解释为 lot 调度状态。

当前 demo 中：

- `O-INSERT-001` 是插单 lot 示例。
- 该插单 lot 对应产品 `P-B`，而 `P-B` 具有重复经过 `CNC` 设备组的可重入路线。

## 4. 当前影响分析如何兼容半导体

当前影响分析逻辑可以映射为半导体插单 lot 的影响范围识别：

1. 查询插单 lot 对应的产品类型。
2. 根据产品类型展开 route steps。
3. 获取该插单 lot 需要经过的 tool group 集合。
4. 在当前 ACTIVE 方案中筛选：
   - 计划开始时间不早于插单 lot 的 `release_time`；
   - 任务所在 `equipment_group_id` 属于插单 lot 所需 tool groups；
   - 任务未冻结。
5. 这些任务被视为受影响 lot-step。
6. 根据受影响 lot-step 数量判断影响等级。

该逻辑适合 MVP 阶段验证：

- 插单 lot 会占用哪些 tool groups；
- 当前计划中哪些 lot-step 可能与插单 lot 竞争同类设备能力；
- 哪些任务应进入局部重调度集合。

## 5. 当前局部重调度如何兼容半导体

当前局部重调度逻辑可解释为半导体 hot lot 插入后的局部重排：

- 未受影响 lot-step 作为冻结任务，保持原计划不变。
- 受影响 lot-step 进入可调整集合。
- 插单 lot 的 route steps 展开为 insertTasks，并加入可调整集合。
- 调度算法保持 step 顺序约束，即同一 lot 后续 step 不能早于前序 step 完成。
- 调度算法保持 tool 能力约束，即同一 tool 同一时间只能加工一个任务。
- 新方案保留完整任务集合，并通过 `is_inserted`、`is_changed`、`is_frozen` 区分插单任务、变更任务和冻结任务。

这使系统可以演示半导体可重入场景下的核心决策闭环：

插单 lot 到达 -> 识别受影响 tool groups 和 lot-steps -> 冻结未受影响任务 -> 重排受影响任务和插单 lot steps -> 生成新方案 -> KPI 和甘特图对比 -> 人工确认。

## 6. 当前系统边界

当前 MVP 仍是通用车间调度原型，对半导体场景采用抽象映射方式，尚未显式建模以下对象和约束：

- 独立 `aps_lot` 表。
- wafer 数量。
- recipe。
- mask / reticle。
- layer。
- Q-time / time window。
- batch processing。
- setup / changeover。
- tool qualification。
- chamber / sub-tool。
- rework / hold / release。
- dispatching rule 与 Fab 执行层接口。

当前设计选择：

- MVP 阶段将 `aps_order` 抽象为 lot。
- 将 `aps_route_operation` 抽象为 lot route steps。
- 将 `equipment_group` 抽象为 tool group。
- 将 `equipment` 抽象为 tool。
- 将 `schedule_task` 抽象为 lot-step task。

## 7. 后续半导体方向扩展建议

后续如果继续面向半导体可重入车间深化，可逐步扩展：

- 新增 `aps_lot`，区分 customer order 与 wafer lot。
- 增加 route、recipe、layer、mask、Q-time 字段。
- 增加 tool qualification，表达某个 step 可使用哪些 tool。
- 增加 batch 设备约束，支持炉管等批处理设备。
- 增加 setup / changeover 约束。
- 增加 hold、release、rework 等 Fab 执行状态。
- 增加更适合 reentrant flow shop 的调度算法与评价指标。

当前系统不需要推翻，只需在现有订单、工艺路线、设备组和任务模型上逐步增强半导体特有语义。
