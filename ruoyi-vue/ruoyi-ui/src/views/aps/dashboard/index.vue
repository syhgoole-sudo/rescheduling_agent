<template>
  <div class="app-container aps-dashboard" v-loading="loading">
    <el-card shadow="never" class="scene-card">
      <div slot="header" class="panel-header">
        <div>
          <span class="panel-title">当前调度场景</span>
          <span class="panel-subtitle">共享 Tool 约束下的全局生产调度状态</span>
        </div>
        <el-button
          type="primary"
          plain
          size="mini"
          icon="el-icon-refresh"
          :loading="refreshLoading"
          @click="loadDashboard"
        >刷新</el-button>
      </div>

      <el-row :gutter="20" class="scene-grid">
        <el-col v-for="item in sceneItems" :key="item.label" :xs="24" :sm="12" :md="8" :lg="4">
          <div class="scene-item">
            <div class="scene-label">{{ item.label }}</div>
            <div class="scene-value" :title="item.value">{{ item.value }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <div class="section-heading">
      <span>生产核心指标</span>
      <small>当前生产与有效方案快照</small>
    </div>
    <el-row :gutter="14" class="metric-grid">
      <el-col v-for="metric in metricCards" :key="metric.key" :xs="12" :sm="12" :md="6" :lg="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" :class="'metric-icon--' + metric.tone">
              <i :class="metric.icon"></i>
            </div>
            <div class="metric-body">
              <div class="metric-label">{{ metric.label }}</div>
              <div class="metric-value">
                {{ metric.value }}<span v-if="metric.unit" class="metric-unit">{{ metric.unit }}</span>
              </div>
              <div class="metric-note">{{ metric.note }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="panel-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="dashboard-panel chart-panel">
          <div slot="header" class="panel-header">
            <div>
              <span class="panel-title">生产运行结构</span>
              <span class="panel-subtitle">Lot 构成与当前方案 Tool 负载</span>
            </div>
          </div>
          <el-row :gutter="18">
            <el-col :xs="24" :md="9" class="chart-column">
              <div class="chart-title">Lot 类型构成</div>
              <el-empty v-if="lotTotal === 0" :image-size="72" description="暂无 Lot 数据" />
              <div v-else ref="lotChart" class="dashboard-chart"></div>
            </el-col>
            <el-col :xs="24" :md="15" class="chart-column chart-column--bordered">
              <div class="chart-title">当前方案 Tool 负载 Top 8</div>
              <el-empty v-if="toolLoadData.length === 0" :image-size="72" description="当前方案暂无 Lot-Step 任务" />
              <div v-else ref="toolChart" class="dashboard-chart"></div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="dashboard-panel plan-panel">
          <div slot="header" class="panel-header">
            <div>
              <span class="panel-title">当前调度方案</span>
              <el-tag v-if="currentPlan" size="mini" :type="planTypeTag(currentPlan.planType)">
                {{ currentPlan.planType || '-' }}
              </el-tag>
            </div>
            <el-tag v-if="currentPlan" size="mini" :type="planStatusTag(currentPlan.planStatus)">
              {{ currentPlan.planStatus || '-' }}
            </el-tag>
          </div>

          <el-empty v-if="!currentPlan" :image-size="72" description="暂无当前有效方案" />
          <template v-else>
            <div class="plan-code">{{ currentPlan.planCode }}</div>
            <el-descriptions :column="2" size="small" class="plan-descriptions">
              <el-descriptions-item label="方案类型">{{ currentPlan.planType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="当前算法">{{ currentAlgorithm }}</el-descriptions-item>
              <el-descriptions-item label="方案生成时间" :span="2">{{ formatTime(currentPlan.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="Lot-Step任务数">{{ currentPlanTaskCount }}</el-descriptions-item>
              <el-descriptions-item label="Makespan">{{ formatMinutes(currentMakespanMinutes) }}</el-descriptions-item>
              <el-descriptions-item label="真实交期延期">{{ formatMinutes(currentTrueDelayMinutes) }}</el-descriptions-item>
              <el-descriptions-item label="计划稳定性扰动">{{ formatMinutes(currentStabilityDelayMinutes) }}</el-descriptions-item>
            </el-descriptions>

            <div class="task-flags">
              <span><i class="flag-dot flag-dot--inserted"></i>插单 {{ taskTypeCounts.inserted }}</span>
              <span><i class="flag-dot flag-dot--changed"></i>变更 {{ taskTypeCounts.changed }}</span>
              <span><i class="flag-dot flag-dot--frozen"></i>冻结 {{ taskTypeCounts.frozen }}</span>
            </div>

            <div class="plan-actions">
              <el-button type="primary" size="mini" icon="el-icon-data-analysis" @click="openGantt">查看甘特图</el-button>
              <el-button size="mini" icon="el-icon-s-data" :disabled="currentPlan.planType !== 'RESCHEDULE'" @click="openCompare">查看方案对比</el-button>
              <el-button size="mini" icon="el-icon-document" :disabled="!currentEvent" @click="openExplanation">查看解释报告</el-button>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="panel-row">
      <el-col :xs="24" :lg="17">
        <el-card shadow="never" class="dashboard-panel event-panel">
          <div slot="header" class="panel-header">
            <div>
              <span class="panel-title">插单动态监控</span>
              <span class="panel-subtitle">最近发生的 Hot Lot 插单事件</span>
            </div>
            <el-button type="text" icon="el-icon-right" @click="openInsertEvents">进入插单事件</el-button>
          </div>
          <el-table :data="recentEvents" size="small" :height="318" empty-text="暂无插单事件">
            <el-table-column label="插单 Lot" prop="insertOrderCode" min-width="150" show-overflow-tooltip />
            <el-table-column label="产品" prop="triggerProductCode" min-width="130" show-overflow-tooltip />
            <el-table-column label="插单发生时间" min-width="165">
              <template slot-scope="scope">{{ formatTime(scope.row.eventTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110" align="center">
              <template slot-scope="scope">
                <el-tag size="mini" :type="eventStatusTag(scope.row.eventStatus)">{{ scope.row.eventStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="影响 Lot" width="90" align="center">
              <template slot-scope="scope">{{ eventImpactValue(scope.row, 'affectedOrderCount') }}</template>
            </el-table-column>
            <el-table-column label="影响 Tool" width="95" align="center">
              <template slot-scope="scope">{{ eventImpactValue(scope.row, 'affectedEquipmentCount') }}</template>
            </el-table-column>
            <el-table-column label="当前方案" min-width="160" show-overflow-tooltip>
              <template slot-scope="scope">{{ scope.row.newPlanCode || scope.row.sourcePlanCode || '-' }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="7">
        <el-card shadow="never" class="dashboard-panel fab-panel">
          <div slot="header" class="panel-header">
            <div>
              <span class="panel-title">半导体场景信息</span>
              <span class="panel-subtitle">Fab 调度对象规模</span>
            </div>
          </div>
          <div class="fab-stat-list">
            <div v-for="item in fabStats" :key="item.label" class="fab-stat-item">
              <div class="fab-stat-label"><i :class="item.icon"></i>{{ item.label }}</div>
              <div class="fab-stat-value">{{ item.value }}</div>
            </div>
          </div>
          <div class="fab-context">
            <div class="fab-context-title">当前排程窗口</div>
            <div>{{ formatTime(currentPlan && currentPlan.scheduleStartTime) }}</div>
            <div class="fab-context-separator">至</div>
            <div>{{ formatTime(currentPlan && currentPlan.scheduleEndTime) }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog title="当前方案 KPI 对比" :visible.sync="compareOpen" width="760px" append-to-body>
      <el-empty v-if="!currentCompare" :image-size="72" description="暂无方案对比数据" />
      <el-descriptions v-else :column="2" border size="small">
        <el-descriptions-item label="原方案">{{ comparePlanCode('sourcePlan') }}</el-descriptions-item>
        <el-descriptions-item label="新方案">{{ comparePlanCode('newPlan') }}</el-descriptions-item>
        <el-descriptions-item label="变更 Lot-Step">{{ compareSummary.changedTaskCount }}</el-descriptions-item>
        <el-descriptions-item label="变更比例">{{ formatRatio(compareSummary.changedTaskRatio) }}</el-descriptions-item>
        <el-descriptions-item label="插单真实延期">{{ formatMinutes(compareSummary.insertOrderTrueDelayMinutes) }}</el-descriptions-item>
        <el-descriptions-item label="新真实延期 Lot">{{ compareSummary.newTrueDelayOrderCount }}</el-descriptions-item>
        <el-descriptions-item label="计划后移 Lot">{{ compareSummary.stabilityDelayOrderCount }}</el-descriptions-item>
        <el-descriptions-item label="计划总后移">{{ formatMinutes(compareSummary.stabilityTotalDelayMinutes) }}</el-descriptions-item>
        <el-descriptions-item label="Makespan变化" :span="2">{{ formatSignedMinutes(compareSummary.makespanDiffMinutes) }}</el-descriptions-item>
        <el-descriptions-item label="辅助结论" :span="2">{{ currentCompare.conclusion || '-' }}</el-descriptions-item>
      </el-descriptions>
      <span slot="footer">
        <el-button @click="compareOpen = false">关闭</el-button>
        <el-button type="primary" @click="openGanttCompare">打开新旧甘特图对比</el-button>
      </span>
    </el-dialog>

    <el-dialog title="插单重调度解释报告" :visible.sync="explanationOpen" width="860px" append-to-body>
      <el-descriptions v-if="explanationDetail" :column="2" border size="small">
        <el-descriptions-item label="事件编码">{{ explanationDetail.eventCode || (currentEvent && currentEvent.eventCode) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前方案">{{ currentPlan ? currentPlan.planCode : '-' }}</el-descriptions-item>
        <el-descriptions-item label="插单事件概述" :span="2">{{ explanationDetail.eventSummary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="影响范围说明" :span="2">{{ explanationDetail.impactSummary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="KPI 对比说明" :span="2">{{ explanationDetail.kpiSummary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="调度建议" :span="2">{{ explanationDetail.recommendation || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-input class="report-text" type="textarea" :rows="12" :value="explanationDetail ? explanationDetail.fullReport : ''" readonly />
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from "echarts";
import { parseTime } from "@/utils/ruoyi";
import { listProduct } from "@/api/aps/product";
import { listOrder } from "@/api/aps/order";
import { listEquipment } from "@/api/aps/equipment";
import { listRouteOperation } from "@/api/aps/routeOperation";
import { listSchedulePlan, compareSchedulePlan } from "@/api/aps/schedulePlan";
import { listScheduleTaskByPlan } from "@/api/aps/scheduleTask";
import { listInsertEvent, generateExplanationReport } from "@/api/aps/insertEvent";

export default {
  name: "ApsDashboard",
  data() {
    return {
      loading: true,
      refreshLoading: false,
      initialized: false,
      systemTime: new Date(),
      clockTimer: null,
      productTotal: 0,
      lotTotal: 0,
      normalLotTotal: 0,
      hotLotTotal: 0,
      toolTotal: 0,
      reentrantRouteTotal: 0,
      plans: [],
      events: [],
      currentPlan: null,
      currentEvent: null,
      latestReschedulePlan: null,
      currentTasks: [],
      currentCompare: null,
      lotChart: null,
      toolChart: null,
      compareOpen: false,
      explanationOpen: false,
      explanationDetail: null
    };
  },
  computed: {
    currentKpi() {
      return this.parseJson(this.currentPlan && this.currentPlan.kpiJson) || {};
    },
    currentAlgorithm() {
      const name = String((this.currentPlan && this.currentPlan.algorithmName) || "").toUpperCase();
      if (name.indexOf("GA") !== -1) return "GA";
      if (name.indexOf("RULE") !== -1) return "RULE";
      return name || "-";
    },
    currentRandomSeed() {
      if (this.currentAlgorithm !== "GA") return "不适用";
      const nestedKpi = this.currentKpi.kpi || {};
      return this.firstValue(this.currentKpi.randomSeed, nestedKpi.randomSeed, 42);
    },
    sceneItems() {
      return [
        { label: "系统当前时间", value: this.formatTime(this.systemTime) },
        { label: "当前有效方案", value: this.currentPlan ? this.currentPlan.planCode : "-" },
        { label: "当前方案类型", value: this.currentPlan ? (this.currentPlan.planType || "-") : "-" },
        { label: "最近一次重调度时间", value: this.formatTime(this.latestReschedulePlan && this.latestReschedulePlan.createTime) },
        { label: "当前算法", value: this.currentAlgorithm },
        { label: "randomSeed", value: String(this.currentRandomSeed) }
      ];
    },
    currentPlanTaskCount() {
      if (this.currentTasks.length) return this.currentTasks.length;
      return Number((this.currentPlan && this.currentPlan.totalTaskCount) || 0);
    },
    currentAffectedLotCount() {
      return Number(this.eventImpactValue(this.currentEvent, "affectedOrderCount", 0));
    },
    currentMakespanMinutes() {
      const compare = (this.currentCompare && this.currentCompare.makespanCompare) || {};
      return this.firstNumber(
        this.currentKpi.makespanMinutes,
        this.currentKpi.makespan,
        compare.newMakespanMinutes,
        this.minutesBetween(this.currentPlan && this.currentPlan.scheduleStartTime, this.currentPlan && this.currentPlan.scheduleEndTime)
      );
    },
    currentTrueDelayData() {
      return this.currentKpi.trueDelay || this.currentKpi.trueDelayCompare ||
        ((this.currentCompare && this.currentCompare.trueDelayCompare) || {});
    },
    currentStabilityData() {
      return this.currentKpi.stabilityDelay || this.currentKpi.stabilityCompare ||
        ((this.currentCompare && this.currentCompare.stabilityCompare) || {});
    },
    currentTrueDelayMinutes() {
      return this.firstNumber(
        this.currentTrueDelayData.trueTotalDelayMinutes,
        this.currentTrueDelayData.newTrueTotalDelayMinutes,
        this.currentKpi.trueTotalDelayMinutes,
        this.currentKpi.totalDelayMinutes,
        0
      );
    },
    currentStabilityDelayMinutes() {
      return this.firstNumber(
        this.currentStabilityData.stabilityTotalDelayMinutes,
        this.currentKpi.stabilityTotalDelayMinutes,
        0
      );
    },
    delayedLotCount() {
      return this.firstNumber(
        this.currentTrueDelayData.trueDelayOrderCount,
        this.currentTrueDelayData.newTrueDelayOrderCount,
        this.currentKpi.trueDelayOrderCount,
        this.currentKpi.delayOrderCount,
        0
      );
    },
    toolUtilization() {
      if (!this.currentTasks.length || !this.toolTotal) return 0;
      const fallbackWindow = this.taskWindow();
      const start = this.toTimestamp((this.currentPlan && this.currentPlan.scheduleStartTime) || fallbackWindow.start);
      const end = this.toTimestamp((this.currentPlan && this.currentPlan.scheduleEndTime) || fallbackWindow.end);
      const windowMinutes = start && end && end > start ? (end - start) / 60000 : 0;
      if (!windowMinutes) return 0;
      const occupiedMinutes = this.currentTasks.reduce((sum, task) => sum + this.taskDurationMinutes(task), 0);
      return Math.min(100, occupiedMinutes / (this.toolTotal * windowMinutes) * 100);
    },
    metricCards() {
      return [
        { key: "products", label: "产品数量", value: this.productTotal, unit: "", note: "当前维护产品", icon: "el-icon-s-grid", tone: "blue" },
        { key: "lots", label: "Lot数量", value: this.lotTotal, unit: "", note: "全部生产 Lot", icon: "el-icon-box", tone: "slate" },
        { key: "hotLots", label: "Hot Lot数量", value: this.hotLotTotal, unit: "", note: "INSERT 优先 Lot", icon: "el-icon-warning-outline", tone: "red" },
        { key: "tasks", label: "当前方案任务数量", value: this.currentPlanTaskCount, unit: "", note: "Lot-Step任务", icon: "el-icon-s-operation", tone: "purple" },
        { key: "tools", label: "Tool数量", value: this.toolTotal, unit: "", note: "当前可排程 Tool", icon: "el-icon-monitor", tone: "green" },
        { key: "affected", label: "当前受影响Lot数量", value: this.currentAffectedLotCount, unit: "", note: "最近事件影响范围", icon: "el-icon-connection", tone: "orange" },
        { key: "delayed", label: "延期Lot数量", value: this.delayedLotCount, unit: "", note: "相对真实交期", icon: "el-icon-time", tone: "red" },
        { key: "utilization", label: "Tool利用率", value: this.toolUtilization.toFixed(1), unit: "%", note: "当前排程窗口估算", icon: "el-icon-data-line", tone: "blue" }
      ];
    },
    recentEvents() {
      return this.sortByTime(this.events, "eventTime").slice(0, 8);
    },
    fabStats() {
      return [
        { label: "当前产品数量", value: this.productTotal, icon: "el-icon-s-grid" },
        { label: "当前Lot数量", value: this.lotTotal, icon: "el-icon-box" },
        { label: "当前Tool数量", value: this.toolTotal, icon: "el-icon-monitor" },
        { label: "当前Reentrant Route数量", value: this.reentrantRouteTotal, icon: "el-icon-refresh" }
      ];
    },
    taskTypeCounts() {
      const result = { inserted: 0, changed: 0, frozen: 0, normal: 0 };
      this.currentTasks.forEach(task => {
        if (task.isInserted === "Y") result.inserted += 1;
        else if (task.isChanged === "Y") result.changed += 1;
        else if (task.isFrozen === "Y") result.frozen += 1;
        else result.normal += 1;
      });
      return result;
    },
    toolLoadData() {
      const loadByTool = {};
      this.currentTasks.forEach(task => {
        const key = task.equipmentCode || task.equipmentId || "未分配Tool";
        loadByTool[key] = (loadByTool[key] || 0) + this.taskDurationMinutes(task);
      });
      return Object.keys(loadByTool)
        .map(name => ({ name: String(name), hours: Number((loadByTool[name] / 60).toFixed(2)) }))
        .sort((a, b) => b.hours - a.hours)
        .slice(0, 8);
    },
    compareSummary() {
      const detail = this.currentCompare || {};
      const summary = detail.summary || {};
      const insertOrder = detail.insertOrderCompare || {};
      const trueDelay = detail.trueDelayCompare || detail.delayCompare || {};
      const stability = detail.stabilityCompare || {};
      const makespan = detail.makespanCompare || {};
      return {
        changedTaskCount: this.firstNumber(summary.changedTaskCount, detail.changedTaskCount, 0),
        changedTaskRatio: this.firstNumber(summary.changedTaskRatio, detail.changedTaskRatio, 0),
        insertOrderTrueDelayMinutes: this.firstNumber(insertOrder.insertOrderTrueDelayMinutes, insertOrder.insertOrderDelayMinutes, 0),
        newTrueDelayOrderCount: this.firstNumber(trueDelay.newTrueDelayOrderCount, trueDelay.newDelayOrderCount, 0),
        stabilityDelayOrderCount: this.firstNumber(stability.stabilityDelayOrderCount, 0),
        stabilityTotalDelayMinutes: this.firstNumber(stability.stabilityTotalDelayMinutes, 0),
        makespanDiffMinutes: this.firstNumber(makespan.makespanDiffMinutes, 0)
      };
    }
  },
  mounted() {
    this.clockTimer = window.setInterval(() => {
      this.systemTime = new Date();
    }, 1000);
    window.addEventListener("resize", this.resizeCharts);
    this.loadDashboard();
  },
  beforeDestroy() {
    if (this.clockTimer) window.clearInterval(this.clockTimer);
    window.removeEventListener("resize", this.resizeCharts);
    this.disposeCharts();
  },
  methods: {
    loadDashboard() {
      if (!this.initialized) this.loading = true;
      this.refreshLoading = true;
      const requests = {
        products: listProduct({ pageNum: 1, pageSize: 1, status: "0" }),
        lots: listOrder({ pageNum: 1, pageSize: 1 }),
        normalLots: listOrder({ pageNum: 1, pageSize: 1, orderType: "NORMAL" }),
        hotLots: listOrder({ pageNum: 1, pageSize: 1, orderType: "INSERT" }),
        tools: listEquipment({ pageNum: 1, pageSize: 1, status: "0" }),
        routes: listRouteOperation({ pageNum: 1, pageSize: 5000 }),
        plans: listSchedulePlan({ pageNum: 1, pageSize: 200, orderByColumn: "createTime", isAsc: "desc" }),
        events: listInsertEvent({ pageNum: 1, pageSize: 100, orderByColumn: "eventTime", isAsc: "desc" })
      };
      const keys = Object.keys(requests);
      return Promise.all(keys.map(key => this.safeRequest(key, requests[key]))).then(results => {
        const data = {};
        const failed = [];
        results.forEach(result => {
          if (result.error) failed.push(result.key);
          else data[result.key] = result.response;
        });
        this.productTotal = this.responseTotal(data.products);
        this.lotTotal = this.responseTotal(data.lots);
        this.normalLotTotal = this.responseTotal(data.normalLots);
        this.hotLotTotal = this.responseTotal(data.hotLots);
        this.toolTotal = this.responseTotal(data.tools);
        this.reentrantRouteTotal = this.calculateReentrantRouteCount(this.responseRows(data.routes));
        this.plans = this.sortByTime(this.responseRows(data.plans), "createTime");
        this.events = this.sortByTime(this.responseRows(data.events), "eventTime");
        this.selectCurrentContext();
        if (failed.length) this.$modal.msgWarning("部分驾驶舱数据暂未加载，请检查当前账号的 APS 查询权限。");
        return this.loadCurrentPlanDetails();
      }).then(() => {
        this.$nextTick(this.renderCharts);
      }).finally(() => {
        this.loading = false;
        this.refreshLoading = false;
        this.initialized = true;
      });
    },
    safeRequest(key, promise) {
      return promise.then(response => ({ key, response })).catch(error => ({ key, error }));
    },
    selectCurrentContext() {
      const activePlans = this.plans.filter(plan => plan.activeFlag === "Y" && plan.planStatus !== "HISTORY" && plan.planStatus !== "REJECTED");
      this.currentPlan = activePlans[0] || this.plans[0] || null;
      this.latestReschedulePlan = this.plans.find(plan => plan.planType === "RESCHEDULE") || null;
      if (!this.currentPlan) {
        this.currentEvent = this.events[0] || null;
        return;
      }
      this.currentEvent = this.events.find(event =>
        String(event.eventId) === String(this.currentPlan.eventId) ||
        String(event.newPlanId) === String(this.currentPlan.planId) ||
        String(event.sourcePlanId) === String(this.currentPlan.planId)
      ) || this.events[0] || null;
    },
    loadCurrentPlanDetails() {
      this.currentTasks = [];
      this.currentCompare = null;
      if (!this.currentPlan || !this.currentPlan.planId) return Promise.resolve();
      const requests = [
        this.safeRequest("tasks", listScheduleTaskByPlan(this.currentPlan.planId))
      ];
      if (this.currentPlan.planType === "RESCHEDULE") {
        requests.push(this.safeRequest("compare", compareSchedulePlan(this.currentPlan.planId)));
      }
      return Promise.all(requests).then(results => {
        results.forEach(result => {
          if (result.error) return;
          if (result.key === "tasks") this.currentTasks = result.response.data || result.response.rows || [];
          if (result.key === "compare") this.currentCompare = result.response.data || null;
        });
      });
    },
    calculateReentrantRouteCount(operations) {
      const productGroups = {};
      operations.forEach(operation => {
        const productKey = operation.productId || operation.productCode;
        const groupKey = operation.equipmentGroupId || operation.equipmentGroupCode;
        if (!productKey || !groupKey) return;
        if (!productGroups[productKey]) productGroups[productKey] = {};
        productGroups[productKey][groupKey] = (productGroups[productKey][groupKey] || 0) + 1;
      });
      return Object.keys(productGroups).filter(productKey =>
        Object.keys(productGroups[productKey]).some(groupKey => productGroups[productKey][groupKey] > 1)
      ).length;
    },
    responseRows(response) {
      return response ? (response.rows || response.data || []) : [];
    },
    responseTotal(response) {
      if (!response) return 0;
      if (response.total !== undefined && response.total !== null) return Number(response.total);
      return this.responseRows(response).length;
    },
    parseJson(value) {
      if (!value) return {};
      if (typeof value === "object") return value;
      try {
        return JSON.parse(value);
      } catch (e) {
        return {};
      }
    },
    eventImpactValue(event, key, fallback) {
      if (!event) return fallback !== undefined ? fallback : "-";
      const impact = this.parseJson(event.impactJson);
      const value = impact[key];
      return value !== undefined && value !== null ? value : (fallback !== undefined ? fallback : "-");
    },
    firstValue() {
      for (let i = 0; i < arguments.length; i += 1) {
        if (arguments[i] !== undefined && arguments[i] !== null && arguments[i] !== "") return arguments[i];
      }
      return null;
    },
    firstNumber() {
      const value = this.firstValue.apply(this, arguments);
      return value === null || Number.isNaN(Number(value)) ? 0 : Number(value);
    },
    toTimestamp(value) {
      if (!value) return 0;
      if (value instanceof Date) return value.getTime();
      const timestamp = new Date(String(value).replace(/-/g, "/")).getTime();
      return Number.isNaN(timestamp) ? 0 : timestamp;
    },
    sortByTime(items, field) {
      return (items || []).slice().sort((a, b) => {
        const timeDiff = this.toTimestamp(b[field]) - this.toTimestamp(a[field]);
        if (timeDiff !== 0) return timeDiff;
        return Number(b.planId || b.eventId || 0) - Number(a.planId || a.eventId || 0);
      });
    },
    formatTime(value) {
      return value ? parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "-";
    },
    minutesBetween(startValue, endValue) {
      const start = this.toTimestamp(startValue);
      const end = this.toTimestamp(endValue);
      return start && end && end >= start ? (end - start) / 60000 : 0;
    },
    taskDurationMinutes(task) {
      const plannedDuration = this.minutesBetween(task.plannedStartTime, task.plannedEndTime);
      return plannedDuration || Number(task.duration || 0);
    },
    taskWindow() {
      const starts = this.currentTasks.map(task => this.toTimestamp(task.plannedStartTime)).filter(Boolean);
      const ends = this.currentTasks.map(task => this.toTimestamp(task.plannedEndTime)).filter(Boolean);
      return {
        start: starts.length ? new Date(Math.min.apply(null, starts)) : null,
        end: ends.length ? new Date(Math.max.apply(null, ends)) : null
      };
    },
    formatMinutes(value) {
      if (value === undefined || value === null || value === "") return "-";
      const minutes = Math.round(Number(value));
      if (Number.isNaN(minutes)) return "-";
      if (minutes >= 1440) return `${Math.floor(minutes / 1440)}天 ${Math.floor((minutes % 1440) / 60)}小时`;
      if (minutes >= 60) return `${Math.floor(minutes / 60)}小时 ${minutes % 60}分钟`;
      return `${minutes}分钟`;
    },
    formatSignedMinutes(value) {
      const number = Number(value || 0);
      return `${number > 0 ? "+" : ""}${Math.round(number)}分钟`;
    },
    formatRatio(value) {
      const number = Number(value || 0);
      return `${(number > 1 ? number : number * 100).toFixed(1)}%`;
    },
    planTypeTag(type) {
      return type === "RESCHEDULE" ? "warning" : "primary";
    },
    planStatusTag(status) {
      const types = { ACTIVE: "success", CONFIRMED: "success", PENDING: "warning", REJECTED: "danger", HISTORY: "info" };
      return types[status] || "info";
    },
    eventStatusTag(status) {
      const types = { NEW: "info", ANALYZED: "primary", RESCHEDULED: "warning", CONFIRMED: "success", REJECTED: "danger" };
      return types[status] || "info";
    },
    renderCharts() {
      this.renderLotChart();
      this.renderToolChart();
    },
    renderLotChart() {
      if (!this.$refs.lotChart || !this.lotTotal) {
        if (this.lotChart) {
          this.lotChart.dispose();
          this.lotChart = null;
        }
        return;
      }
      if (!this.lotChart || this.lotChart.isDisposed()) this.lotChart = echarts.init(this.$refs.lotChart);
      this.lotChart.setOption({
        animation: false,
        color: ["#3977a8", "#cf4d4d"],
        tooltip: { trigger: "item", formatter: "{b}<br/>{c} Lot（{d}%）" },
        legend: { bottom: 4, itemWidth: 12, itemHeight: 8 },
        series: [{
          type: "pie",
          radius: ["48%", "70%"],
          center: ["50%", "43%"],
          avoidLabelOverlap: true,
          label: { formatter: "{c}", color: "#303133" },
          data: [
            { name: "普通 Lot", value: this.normalLotTotal },
            { name: "Hot Lot", value: this.hotLotTotal }
          ]
        }]
      }, true);
    },
    renderToolChart() {
      if (!this.$refs.toolChart || !this.toolLoadData.length) {
        if (this.toolChart) {
          this.toolChart.dispose();
          this.toolChart = null;
        }
        return;
      }
      if (!this.toolChart || this.toolChart.isDisposed()) this.toolChart = echarts.init(this.$refs.toolChart);
      const data = this.toolLoadData.slice().reverse();
      this.toolChart.setOption({
        animation: false,
        grid: { left: 18, right: 28, top: 12, bottom: 28, containLabel: true },
        tooltip: { trigger: "axis", axisPointer: { type: "shadow" }, formatter: params => `${params[0].name}<br/>排程负载：${params[0].value} 小时` },
        xAxis: { type: "value", name: "小时", splitLine: { lineStyle: { color: "#edf0f3" } } },
        yAxis: { type: "category", data: data.map(item => item.name), axisTick: { show: false } },
        series: [{
          type: "bar",
          data: data.map(item => item.hours),
          barMaxWidth: 18,
          itemStyle: { color: "#2f8f68" }
        }]
      }, true);
    },
    resizeCharts() {
      if (this.lotChart && !this.lotChart.isDisposed()) this.lotChart.resize();
      if (this.toolChart && !this.toolChart.isDisposed()) this.toolChart.resize();
    },
    disposeCharts() {
      if (this.lotChart) this.lotChart.dispose();
      if (this.toolChart) this.toolChart.dispose();
      this.lotChart = null;
      this.toolChart = null;
    },
    openGantt() {
      if (!this.currentPlan) return;
      this.$router.push({ path: "/aps/scheduleTask/gantt", query: { planId: this.currentPlan.planId } });
    },
    openCompare() {
      if (!this.currentPlan || this.currentPlan.planType !== "RESCHEDULE") return;
      const show = () => {
        this.compareOpen = true;
      };
      if (this.currentCompare) {
        show();
        return;
      }
      compareSchedulePlan(this.currentPlan.planId).then(response => {
        this.currentCompare = response.data || null;
        show();
      });
    },
    openGanttCompare() {
      this.compareOpen = false;
      this.$router.push({ path: "/aps/scheduleTask/ganttCompare", query: { planId: this.currentPlan.planId } });
    },
    openExplanation() {
      if (!this.currentEvent) return;
      generateExplanationReport(this.currentEvent.eventId).then(response => {
        this.explanationDetail = response.data || {};
        this.explanationOpen = true;
      });
    },
    openInsertEvents() {
      this.$router.push({ path: "/aps/insertEvent" });
    },
    comparePlanCode(key) {
      const plan = this.currentCompare && this.currentCompare[key];
      return plan ? (plan.planCode || plan.planId || "-") : "-";
    }
  }
};
</script>

<style scoped lang="scss">
.aps-dashboard {
  min-height: calc(100vh - 84px);
  background: #f4f6f8;
}

.scene-card,
.dashboard-panel,
.metric-card {
  border-color: #dfe4ea;
  border-radius: 6px;
}

.scene-card {
  border-top: 3px solid #2f6f9f;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 28px;
  gap: 12px;
}

.panel-title {
  color: #263746;
  font-size: 16px;
  font-weight: 600;
  margin-right: 10px;
}

.panel-subtitle {
  color: #8795a1;
  font-size: 12px;
}

.scene-grid {
  margin-bottom: -8px;
}

.scene-item {
  min-height: 64px;
  padding: 8px 0 10px;
}

.scene-label {
  color: #84919d;
  font-size: 12px;
  margin-bottom: 9px;
}

.scene-value {
  color: #1f2d3d;
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-heading {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 18px 0 10px;
  color: #263746;
  font-size: 16px;
  font-weight: 600;
}

.section-heading small {
  color: #8a97a2;
  font-size: 12px;
  font-weight: 400;
}

.metric-grid {
  margin-bottom: -14px;
}

.metric-grid .el-col {
  margin-bottom: 14px;
}

.metric-card {
  height: 118px;
}

.metric-content {
  display: flex;
  align-items: center;
  height: 76px;
}

.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 48px;
  width: 48px;
  height: 48px;
  margin-right: 14px;
  border-radius: 6px;
  font-size: 23px;
}

.metric-icon--blue { color: #2f6f9f; background: #e7f0f7; }
.metric-icon--slate { color: #52677a; background: #edf1f4; }
.metric-icon--red { color: #bd4242; background: #f9eaea; }
.metric-icon--purple { color: #76569b; background: #f0ebf5; }
.metric-icon--green { color: #2f8060; background: #e7f3ed; }
.metric-icon--orange { color: #b96d22; background: #faeee2; }

.metric-body {
  min-width: 0;
}

.metric-label {
  color: #667684;
  font-size: 13px;
}

.metric-value {
  color: #1f2d3d;
  font-size: 25px;
  font-weight: 600;
  line-height: 34px;
}

.metric-unit {
  margin-left: 3px;
  color: #637381;
  font-size: 13px;
  font-weight: 400;
}

.metric-note {
  color: #9aa5ae;
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.panel-row {
  margin-top: 14px;
}

.dashboard-panel {
  min-height: 402px;
}

.chart-column {
  min-height: 310px;
}

.chart-column--bordered {
  border-left: 1px solid #edf0f3;
}

.chart-title {
  color: #52606d;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  margin: 2px 0 4px;
}

.dashboard-chart {
  width: 100%;
  height: 286px;
}

.plan-code {
  padding: 4px 0 14px;
  color: #245f8c;
  font-size: 18px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-descriptions {
  min-height: 180px;
}

.task-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  padding: 13px 0;
  color: #657481;
  font-size: 12px;
  border-top: 1px solid #edf0f3;
}

.flag-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 5px;
  border-radius: 50%;
}

.flag-dot--inserted { background: #cf4d4d; }
.flag-dot--changed { background: #cc7a29; }
.flag-dot--frozen { background: #38966d; }

.plan-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.plan-actions .el-button + .el-button {
  margin-left: 0;
}

.event-panel,
.fab-panel {
  min-height: 394px;
}

.fab-stat-list {
  border-top: 1px solid #edf0f3;
}

.fab-stat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 52px;
  border-bottom: 1px solid #edf0f3;
}

.fab-stat-label {
  color: #5e6f7d;
  font-size: 13px;
}

.fab-stat-label i {
  width: 22px;
  color: #3977a8;
}

.fab-stat-value {
  color: #1f2d3d;
  font-size: 19px;
  font-weight: 600;
}

.fab-context {
  margin-top: 17px;
  padding: 12px 14px;
  color: #50606d;
  font-size: 12px;
  line-height: 22px;
  background: #f4f7f9;
  border-left: 3px solid #2f6f9f;
}

.fab-context-title {
  margin-bottom: 3px;
  color: #2d3d4b;
  font-weight: 600;
}

.fab-context-separator {
  color: #9aa5ae;
}

.report-text {
  margin-top: 12px;
}

@media (max-width: 1199px) {
  .plan-panel,
  .fab-panel {
    margin-top: 14px;
  }
}

@media (max-width: 767px) {
  .aps-dashboard {
    padding: 12px;
  }

  .panel-subtitle {
    display: none;
  }

  .metric-card {
    height: 126px;
  }

  .metric-icon {
    flex-basis: 40px;
    width: 40px;
    height: 40px;
    margin-right: 9px;
  }

  .metric-value {
    font-size: 21px;
  }

  .chart-column--bordered {
    margin-top: 10px;
    border-left: 0;
    border-top: 1px solid #edf0f3;
    padding-top: 14px;
  }
}
</style>
