# 半导体可重入 Hot Lot 插单重调度前端录入数据手册

## 1. 数据主题

半导体可重入光刻 hot lot 插单重调度前端录入数据。

业务解释：

- 订单 = lot / wafer lot。
- 产品 = 半导体产品工艺族。
- 工艺路线工序 = route step。
- 设备组 = tool group。
- 设备 = tool。
- 调度任务 = lot-step task。
- 插单订单 = hot lot。
- 可重入 = 同一 lot 在不同 step 多次进入同一 PHOTO tool group。

本手册只用于通过若依前端页面录入测试数据，不使用 SQL，不修改数据库结构，不新增表。

## 2. 前端录入总顺序

1. 进入“设备组管理”，新增 PHOTO / ETCH / DIFF / CVD / CMP / METRO 六个半导体设备组。
2. 进入“设备管理”，为每个设备组新增 2 台设备。
3. 进入“产品管理”，新增产品 `SEMI-REENTRANT-01`。
4. 进入“工艺路线工序管理”，为该产品新增 10 道工序。
5. 进入“订单管理”，新增 6 个 NORMAL lot。
6. 进入“订单管理”，新增 1 个 INSERT hot lot。
7. 进入“调度方案”页面，重新生成初始调度方案。
8. 查看初始方案任务和初始甘特图。
9. 进入“插单事件”页面，使用 `LOT-FE-HOT-PHOTO-001` 创建并分析插单。
10. 查看影响分析。
11. 点击推荐策略。
12. 点击生成局部重调度方案。
13. 选择 RULE 或 GA。
14. 查看 RESCHEDULE 新方案。
15. 查看 KPI 对比。
16. 查看新旧甘特图对比。
17. 查看 AI 分析报告。

## 3. 设备组管理录入

页面路径：APS 调度系统 -> 设备组管理 -> 新增。

如果页面字段没有“设备组类型”，则不填，将说明写入“备注”。

| 设备组编码 | 设备组名称 | 设备组类型 / 备注 | 状态 |
| --- | --- | --- | --- |
| PHOTO | 光刻设备组 / PHOTO Tool Group | 半导体可重入核心设备组，lot 会多次返回该设备组进行不同层光刻 | 正常 / 启用 |
| ETCH | 刻蚀设备组 / ETCH Tool Group | 用于模拟等离子刻蚀或湿法刻蚀工序 | 正常 / 启用 |
| DIFF | 扩散退火设备组 / DIFF-ANNEAL Tool Group | 用于模拟扩散、氧化、退火等热处理工艺 | 正常 / 启用 |
| CVD | 薄膜沉积设备组 / CVD Tool Group | 用于模拟化学气相沉积薄膜工艺 | 正常 / 启用 |
| CMP | 化学机械抛光设备组 / CMP Tool Group | 用于模拟平坦化工艺 | 正常 / 启用 |
| METRO | 量测检测设备组 / Metrology Tool Group | 用于模拟 CD、Overlay、膜厚、终检等量测检测工序 | 正常 / 启用 |

字段填写建议：

- `status`：选择正常 / 启用。若页面使用代码值，可填 `0`。
- `del_flag`：若页面出现，使用默认值或填 `0`。
- `remark`：填写上表备注。

## 4. 设备管理录入

页面路径：APS 调度系统 -> 设备管理 -> 新增。

如果页面中“所属设备组”是下拉框，请选择对应设备组名称；如果是编码输入框，请填写对应设备组编码。

| 设备编码 | 设备名称 | 所属设备组编码 | 状态 | 备注 |
| --- | --- | --- | --- | --- |
| PHOTO-01 | 光刻机 01 | PHOTO | 正常 / 启用 | 模拟 scanner / track 光刻单元 |
| PHOTO-02 | 光刻机 02 | PHOTO | 正常 / 启用 | 模拟 scanner / track 光刻单元 |
| ETCH-01 | 刻蚀机 01 | ETCH | 正常 / 启用 | 模拟 etcher |
| ETCH-02 | 刻蚀机 02 | ETCH | 正常 / 启用 | 模拟 etcher |
| DIFF-01 | 扩散炉 01 | DIFF | 正常 / 启用 | 模拟 diffusion / anneal tool |
| DIFF-02 | 扩散炉 02 | DIFF | 正常 / 启用 | 模拟 diffusion / anneal tool |
| CVD-01 | CVD 沉积设备 01 | CVD | 正常 / 启用 | 模拟 CVD chamber |
| CVD-02 | CVD 沉积设备 02 | CVD | 正常 / 启用 | 模拟 CVD chamber |
| CMP-01 | CMP 抛光设备 01 | CMP | 正常 / 启用 | 模拟 CMP tool |
| CMP-02 | CMP 抛光设备 02 | CMP | 正常 / 启用 | 模拟 CMP tool |
| METRO-01 | 量测设备 01 | METRO | 正常 / 启用 | 模拟 CD / overlay metrology |
| METRO-02 | 量测设备 02 | METRO | 正常 / 启用 | 模拟 final inspection / metrology |

字段填写建议：

- `equipment_group_id`：如果页面需要选择设备组 ID，请通过下拉选择，不手填 ID。
- `equipment_group_code`：如果页面有该字段，填对应编码，例如 `PHOTO`。
- `status`：正常 / 启用。若页面使用代码值，可填 `0`。
- 没有的字段不填，使用默认值。

## 5. 产品管理录入

页面路径：APS 调度系统 -> 产品管理 -> 新增。

| 字段 | 填写值 |
| --- | --- |
| 产品编码 | SEMI-REENTRANT-01 |
| 产品名称 | 半导体可重入光刻产品 |
| 产品类型 | 半导体，若页面无该字段则不填 |
| 状态 | 正常 / 启用 |
| 备注 | 前端录入的半导体 reentrant flow 测试产品，用于 hot lot 插单重调度和 GA 算法验证 |

说明：

- 如果页面没有“产品类型”字段，不填。
- 保存后，后续工艺路线和订单都选择或填写该产品。

## 6. 工艺路线工序管理录入

页面路径：APS 调度系统 -> 工艺路线工序管理 -> 新增。

产品统一选择或填写：

- 产品编码：`SEMI-REENTRANT-01`

核心可重入特征：

- PHOTO 设备组在 step 2、step 5、step 8 重复出现。
- 这表示同一个 lot 在不同工艺层多次返回 PHOTO tool group。

| 产品编码 | process_seq | process_code | process_name | equipment_group_code | standard_duration（分钟） | 备注 |
| --- | ---: | --- | --- | --- | ---: | --- |
| SEMI-REENTRANT-01 | 1 | CLEAN-L0 | 前清洗 / CLEAN-L0 | METRO | 30 | 模拟前清洗和来料检查，可用 METRO 设备组承载 |
| SEMI-REENTRANT-01 | 2 | PHOTO-L1 | 第1层光刻 / PHOTO-L1 | PHOTO | 60 | 第一次进入 PHOTO 设备组 |
| SEMI-REENTRANT-01 | 3 | ETCH-L1 | 第1层刻蚀 / ETCH-L1 | ETCH | 80 | 第1层图形转移刻蚀 |
| SEMI-REENTRANT-01 | 4 | CVD-L2 | 第2层薄膜沉积 / CVD-L2 | CVD | 70 | 模拟薄膜沉积工序 |
| SEMI-REENTRANT-01 | 5 | PHOTO-L2 | 第2层光刻 / PHOTO-L2 | PHOTO | 60 | 第二次进入 PHOTO 设备组，体现可重入 |
| SEMI-REENTRANT-01 | 6 | DIFF-L2 | 第2层扩散退火 / DIFF-L2 | DIFF | 90 | 模拟扩散 / 氧化 / 退火工艺 |
| SEMI-REENTRANT-01 | 7 | CMP-L2 | 第2层 CMP 平坦化 / CMP-L2 | CMP | 60 | 模拟平坦化工序 |
| SEMI-REENTRANT-01 | 8 | PHOTO-L3 | 第3层光刻 / PHOTO-L3 | PHOTO | 60 | 第三次进入 PHOTO 设备组，体现可重入 |
| SEMI-REENTRANT-01 | 9 | ETCH-L3 | 第3层刻蚀 / ETCH-L3 | ETCH | 80 | 第3层图形转移刻蚀 |
| SEMI-REENTRANT-01 | 10 | FINAL-METRO | 终检量测 / FINAL-METRO | METRO | 30 | 终检和量测 |

字段填写建议：

- `product_id`：如果页面是下拉框，选择 `SEMI-REENTRANT-01` 对应产品。
- `product_code`：如果页面有该字段，填 `SEMI-REENTRANT-01`。
- `equipment_group_id`：如果页面是下拉框，选择对应设备组。
- `equipment_group_code`：如果页面有该字段，填表中编码。
- `status`：正常 / 启用。若页面使用代码值，可填 `0`。
- `del_flag`：若页面出现，使用默认值或填 `0`。

## 7. 普通 Lot / 订单管理录入

页面路径：APS 调度系统 -> 订单管理 -> 新增。

当前系统页面叫“订单”，在半导体语境下这里作为 lot 使用。

统一字段：

- 产品编码：`SEMI-REENTRANT-01`
- 订单类型：`NORMAL`
- 订单状态：`NEW`
- 数量：`25`
- 备注：`前端录入半导体可重入普通 lot`

| 订单编码 / lot 编码 | 产品编码 | 订单类型 | 订单状态 | 数量 | 优先级 | 释放时间 | 交期 | 备注 |
| --- | --- | --- | --- | ---: | ---: | --- | --- | --- |
| LOT-FE-R-001 | SEMI-REENTRANT-01 | NORMAL | NEW | 25 | 2 | 2026-06-18 08:00:00 | 2026-06-19 02:00:00 | 前端录入半导体可重入普通 lot |
| LOT-FE-R-002 | SEMI-REENTRANT-01 | NORMAL | NEW | 25 | 3 | 2026-06-18 08:20:00 | 2026-06-19 03:00:00 | 前端录入半导体可重入普通 lot |
| LOT-FE-R-003 | SEMI-REENTRANT-01 | NORMAL | NEW | 25 | 4 | 2026-06-18 08:40:00 | 2026-06-19 04:00:00 | 前端录入半导体可重入普通 lot |
| LOT-FE-R-004 | SEMI-REENTRANT-01 | NORMAL | NEW | 25 | 2 | 2026-06-18 09:00:00 | 2026-06-19 05:00:00 | 前端录入半导体可重入普通 lot |
| LOT-FE-R-005 | SEMI-REENTRANT-01 | NORMAL | NEW | 25 | 3 | 2026-06-18 09:30:00 | 2026-06-19 06:00:00 | 前端录入半导体可重入普通 lot |
| LOT-FE-R-006 | SEMI-REENTRANT-01 | NORMAL | NEW | 25 | 4 | 2026-06-18 10:00:00 | 2026-06-19 07:00:00 | 前端录入半导体可重入普通 lot |

字段填写建议：

- `product_id`：如果页面是下拉框，选择 `SEMI-REENTRANT-01`。
- `product_code`：如果页面有该字段，填 `SEMI-REENTRANT-01`。
- `quantity`：填 `25`。
- `priority_level`：数字越小优先级越高。
- 没有的字段不填，使用默认值。

## 8. 插单 Hot Lot 录入

页面路径：APS 调度系统 -> 订单管理 -> 新增。

| 字段 | 填写值 |
| --- | --- |
| 订单编码 / lot 编码 | LOT-FE-HOT-PHOTO-001 |
| 产品编码 | SEMI-REENTRANT-01 |
| 订单类型 | INSERT |
| 订单状态 | NEW |
| 数量 | 25 |
| 优先级 | 1 |
| 释放时间 | 2026-06-18 11:00:00 |
| 交期 | 2026-06-19 00:00:00 |
| 备注 | 前端录入半导体 hot lot，用于 GA 局部重调度验证 |

说明：

- 该 hot lot 经过同一条 10 步可重入路线。
- PHOTO tool group 被访问 3 次。
- 总工时较长，交期设置为 `2026-06-19 00:00:00`，用于保证 GA 有机会生成按期或接近按期的可行方案。

## 9. 调度与插单测试流程

1. 进入“调度方案”页面。
2. 点击“生成初始调度”。
3. 确认 `LOT-FE-R-001` 到 `LOT-FE-R-006` 被排入 INITIAL 初始方案。
4. 点击“查看任务”，检查 10 道 route steps 是否生成 lot-step tasks。
5. 点击“查看甘特图”，检查 PHOTO / ETCH / DIFF / CVD / CMP / METRO 对应设备上的任务条。
6. 进入“插单事件”页面。
7. 点击“创建并分析插单”。
8. 输入或选择 `LOT-FE-HOT-PHOTO-001` 对应的订单 ID。
9. 查看影响分析结果。
10. 点击“推荐策略”。
11. 点击“生成局部重调度方案”。
12. 选择 `规则算法 RULE` 或 `遗传算法 GA`。
13. 查看 RESCHEDULE 新方案。
14. 查看 KPI 对比。
15. 查看新旧甘特图对比。
16. 查看 AI 分析报告。
17. 根据结果采用或拒绝方案。

## 10. 测试注意事项

1. 新增前端数据后，必须重新生成初始调度方案。
2. 不要使用旧插单事件测试新 hot lot。
3. 每次测试 GA，建议使用新的插单事件。
4. 如果事件已经是 `RESCHEDULED / CONFIRMED / REJECTED`，则不能再次生成重调度方案。
5. 如果想比较 RULE 和 GA，建议分别创建两个插单事件，或后续扩展“一个事件多个候选方案”的机制。
6. 当前系统的“订单”在半导体语境下解释为 lot。
7. PHOTO 设备组重复出现是可重入特征的关键。
8. 解释报告中要强调 AI Agent 不直接排产，只解释影响、KPI 和方案取舍。

## 11. 演示讲解要点

- 这套数据不再使用 CUT / CNC / HEAT / INSPECT 模拟半导体设备族，而是直接使用 PHOTO / ETCH / DIFF / CVD / CMP / METRO。
- `SEMI-REENTRANT-01` 的工艺路线中 PHOTO 出现 3 次，体现 reentrant flow。
- `LOT-FE-HOT-PHOTO-001` 是 hot lot 插单。
- 影响分析会识别 hot lot 需要占用的 tool groups，并找出当前 ACTIVE 方案中同 tool group、释放时间之后的未冻结 lot-step。
- 局部重调度会冻结未受影响任务，重排受影响 lot-step 和 hot lot steps。
- RULE 和 GA 都不改变系统边界：排产结果由算法生成，AI 只解释，不排产。
