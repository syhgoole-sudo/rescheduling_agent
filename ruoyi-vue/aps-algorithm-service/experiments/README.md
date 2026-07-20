# 调度算法对比实验

本模块在相同局部重调度实例、相同约束解码器和相同 KPI 口径下，对比以下算法：

- `FIFO`：按任务释放时间排序。
- `EDD`：按订单交期排序。
- `SPT`：按订单剩余加工时长排序。
- `RULE`：当前系统的插单优先规则算法。
- `GA`：当前系统的遗传算法。

## 安装依赖

```powershell
pip install -r requirements-experiment.txt
```

## 运行完整实验

```powershell
python -m experiments.run_benchmark --order-counts 20 50 100 --replications 10
python -m experiments.analyze_results
```

完整实验对每个订单规模生成 10 个独立实例。每个实例由五种算法处理，原始结果保存在
`experiments/results/benchmark_results.csv`。

## 快速验证

```powershell
python -m experiments.run_benchmark --order-counts 10 20 --replications 2 --population-size 12 --generations 10 --output experiments/results/smoke_results.csv
python -m experiments.analyze_results --input experiments/results/smoke_results.csv --output-dir experiments/results/smoke
```

## 输出说明

- `benchmark_results.csv`：每个实例、每种算法的原始结果。
- `benchmark_summary.csv`：均值、标准差、中位数、最小值和最大值。
- `improvement_vs_rule.csv`：相对 `RULE` 的改进率，正数表示该指标降低。
- `improvement_vs_fifo.csv`：相对 `FIFO` 的改进率，正数表示该指标降低。
- `*_by_scale.png`：指标随订单规模变化的折线图，阴影表示标准差。
- `total_delay_boxplot.png`：总延期时间分布箱线图。
- `ga_convergence.csv`、`ga_convergence.png`：GA 各规模每代最优目标均值及收敛曲线。

主要指标包括 makespan、总延期时间、插单延期、计划稳定性延迟、任务变更比例和运行时间。
所有指标均为越小越好。实验脚本会校验设备任务不重叠、工序顺序、冻结任务不变、设备资格和任务完整性；存在不可行解时，分析脚本会拒绝生成汇总结论。

## 可复现约定

- 实例随机种子写入每一条原始记录。
- GA 使用与实例相同的随机种子。
- GA 默认参数为种群规模 30、迭代 50 次、交叉率 0.8、变异率 0.15。
- GA 初始种群同时注入 RULE、FIFO、EDD、SPT 启发式解，其余个体由固定随机种子生成。
- GA 使用分层目标：先最小化 hot lot 延期，再加权优化总延期、最大延期、稳定性、makespan 和任务变更数。
- 图表数据来自原始 CSV，不手工修改统计结果。
