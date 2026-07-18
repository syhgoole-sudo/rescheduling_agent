<template>
  <el-card shadow="never" class="gantt-preview-card">
    <div slot="header" class="preview-header">
      <span>调度甘特图预览</span>
      <div class="preview-actions">
        <el-button size="mini" icon="el-icon-data-analysis" :disabled="!sourcePlanId" @click="openFullGantt(sourcePlanId)">完整原方案</el-button>
        <el-button size="mini" icon="el-icon-data-analysis" :disabled="!newPlanId" @click="openFullGantt(newPlanId)">完整候选方案</el-button>
        <el-button size="mini" icon="el-icon-data-line" :disabled="!newPlanId" @click="openGanttCompare">新旧对比</el-button>
      </div>
    </div>

    <div class="preview-legend">
      <span><i class="legend-color inserted"></i>插单任务</span>
      <span><i class="legend-color changed"></i>变更任务</span>
      <span><i class="legend-color frozen"></i>冻结任务</span>
      <span><i class="legend-color normal"></i>普通任务</span>
    </div>

    <el-tabs v-model="activeTab" @tab-click="handleTabChange">
      <el-tab-pane label="原方案" name="source">
        <div v-loading="loading.source" class="preview-state-container">
          <el-empty v-if="!sourcePlanId" description="请先生成或选择初始调度方案。" />
          <el-alert v-else-if="errors.source" type="error" :closable="false" show-icon :title="errors.source" />
          <el-empty v-else-if="!loading.source && sourceEquipments.length === 0" description="当前方案暂无调度任务。" />
          <div v-show="sourcePlanId && !errors.source && sourceEquipments.length > 0" class="chart-scroll">
            <div ref="sourceChart" class="preview-chart" :style="{ height: sourceChartHeight + 'px' }"></div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="候选方案" name="candidate">
        <div v-loading="loading.candidate" class="preview-state-container">
          <el-empty v-if="!newPlanId" description="请先生成重调度候选方案。" />
          <el-alert v-else-if="errors.candidate" type="error" :closable="false" show-icon :title="errors.candidate" />
          <el-empty v-else-if="!loading.candidate && candidateEquipments.length === 0" description="当前方案暂无调度任务。" />
          <div v-show="newPlanId && !errors.candidate && candidateEquipments.length > 0" class="chart-scroll">
            <div ref="candidateChart" class="preview-chart" :style="{ height: candidateChartHeight + 'px' }"></div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script>
import * as echarts from "echarts";
import { getScheduleTaskGantt } from "@/api/aps/scheduleTask";

const TASK_COLORS = {
  INSERTED: "#ef4444",
  CHANGED: "#f59e0b",
  FROZEN: "#22c55e",
  NORMAL: "#3b82f6"
};

export default {
  name: "RescheduleGanttPreview",
  props: {
    sourcePlanId: {
      type: [Number, String],
      default: null
    },
    newPlanId: {
      type: [Number, String],
      default: null
    }
  },
  data() {
    return {
      activeTab: "source",
      sourceEquipments: [],
      candidateEquipments: [],
      sourceChart: null,
      candidateChart: null,
      ganttCache: {},
      pendingRequests: {},
      cacheGeneration: 0,
      loading: {
        source: false,
        candidate: false
      },
      errors: {
        source: null,
        candidate: null
      }
    };
  },
  computed: {
    sourceChartHeight() {
      return Math.max(320, this.sourceEquipments.length * 48 + 120);
    },
    candidateChartHeight() {
      return Math.max(320, this.candidateEquipments.length * 48 + 120);
    }
  },
  watch: {
    sourcePlanId: {
      immediate: true,
      handler(planId) {
        this.handlePlanChange("source", planId);
      }
    },
    newPlanId: {
      immediate: true,
      handler(planId) {
        this.handlePlanChange("candidate", planId);
      }
    }
  },
  mounted() {
    window.addEventListener("resize", this.resizeCharts);
    this.$nextTick(() => this.renderChart(this.activeChartType()));
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.resizeCharts);
    this.disposeCharts();
  },
  methods: {
    handlePlanChange(type, planId) {
      if (!planId) {
        this.clearChartType(type);
        return;
      }
      this.setEquipments(type, []);
      this.$set(this.errors, type, null);
      this.disposeChart(type);
      this.loadPlanGantt(type, planId);
    },
    loadPlanGantt(type, planId) {
      if (!planId) {
        return Promise.resolve();
      }
      const cacheKey = String(planId);
      const requestGeneration = this.cacheGeneration;
      this.$set(this.errors, type, null);
      if (this.ganttCache[cacheKey]) {
        this.applyEquipments(type, planId, this.ganttCache[cacheKey]);
        return Promise.resolve(this.ganttCache[cacheKey]);
      }

      this.$set(this.loading, type, true);
      let request = this.pendingRequests[cacheKey];
      if (!request) {
        request = getScheduleTaskGantt(planId).then(response => {
          const data = response.data || {};
          const equipments = this.normalizeEquipments(data.equipments || []);
          if (requestGeneration === this.cacheGeneration) {
            this.$set(this.ganttCache, cacheKey, equipments);
          }
          return equipments;
        });
        this.$set(this.pendingRequests, cacheKey, request);
      }

      return request.then(equipments => {
        if (requestGeneration !== this.cacheGeneration) {
          return equipments;
        }
        this.applyEquipments(type, planId, equipments);
        return equipments;
      }).catch(() => {
        if (this.currentPlanId(type) === String(planId)) {
          this.$set(this.errors, type, "甘特图数据加载失败，请检查方案任务或后端服务。");
          this.setEquipments(type, []);
          this.disposeChart(type);
        }
        return [];
      }).finally(() => {
        if (this.pendingRequests[cacheKey] === request) {
          this.$delete(this.pendingRequests, cacheKey);
        }
        if (this.currentPlanId(type) === String(planId)) {
          this.$set(this.loading, type, false);
        }
      });
    },
    applyEquipments(type, planId, equipments) {
      if (this.currentPlanId(type) !== String(planId)) {
        return;
      }
      this.setEquipments(type, equipments);
      this.$set(this.loading, type, false);
      if (!equipments.length) {
        this.disposeChart(type);
        return;
      }
      this.$nextTick(() => {
        if (this.activeChartType() === type) {
          this.renderChart(type);
        }
      });
    },
    normalizeEquipments(equipments) {
      return equipments.map(equipment => ({
        ...equipment,
        tasks: (equipment.tasks || []).filter(task => task && task.plannedStartTime && task.plannedEndTime)
      })).filter(equipment => equipment.tasks.length > 0);
    },
    setEquipments(type, equipments) {
      if (type === "source") {
        this.sourceEquipments = equipments;
      } else {
        this.candidateEquipments = equipments;
      }
    },
    currentPlanId(type) {
      const value = type === "source" ? this.sourcePlanId : this.newPlanId;
      return value === null || value === undefined || value === "" ? null : String(value);
    },
    activeChartType() {
      return this.activeTab === "candidate" ? "candidate" : "source";
    },
    handleTabChange() {
      this.$nextTick(() => this.renderChart(this.activeChartType()));
    },
    renderChart(type) {
      const equipments = type === "source" ? this.sourceEquipments : this.candidateEquipments;
      const refName = type === "source" ? "sourceChart" : "candidateChart";
      const chartName = type === "source" ? "sourceChart" : "candidateChart";
      const chartElement = this.$refs[refName];
      if (!chartElement || !equipments.length) {
        return;
      }
      if (!this[chartName] || this[chartName].isDisposed()) {
        this[chartName] = echarts.init(chartElement);
      }

      const categories = equipments.map(equipment =>
        equipment.equipmentCode || equipment.equipmentName || String(equipment.equipmentId || "-")
      );
      const chartTasks = [];
      equipments.forEach((equipment, equipmentIndex) => {
        const equipmentLabel = categories[equipmentIndex];
        equipment.tasks.forEach(task => {
          const start = this.toTime(task.plannedStartTime);
          const end = this.toTime(task.plannedEndTime);
          if (start === null || end === null || end < start) {
            return;
          }
          const taskType = this.getTaskType(task);
          const preparedTask = {
            ...task,
            equipmentLabel,
            taskType,
            displayDuration: task.duration !== null && task.duration !== undefined
              ? task.duration : Math.max(0, Math.round((end - start) / 60000))
          };
          chartTasks.push({
            name: task.orderCode || String(task.taskId || ""),
            value: [start, end, equipmentIndex],
            itemStyle: { color: TASK_COLORS[taskType] },
            task: preparedTask,
            tooltipHtml: this.buildTooltip(preparedTask)
          });
        });
      });

      this[chartName].setOption({
        animation: false,
        tooltip: {
          confine: true,
          formatter: params => params.data.tooltipHtml
        },
        grid: {
          left: 130,
          right: 35,
          top: 24,
          bottom: 58
        },
        dataZoom: [
          {
            type: "inside",
            xAxisIndex: 0,
            filterMode: "weakFilter",
            zoomOnMouseWheel: true,
            moveOnMouseMove: true
          },
          {
            type: "slider",
            xAxisIndex: 0,
            height: 20,
            bottom: 18,
            filterMode: "weakFilter"
          }
        ],
        xAxis: {
          type: "time",
          scale: true,
          axisLabel: { formatter: value => this.formatAxisTime(value) }
        },
        yAxis: {
          type: "category",
          data: categories,
          inverse: true,
          axisLabel: {
            width: 118,
            overflow: "truncate"
          }
        },
        series: [{
          type: "custom",
          renderItem: this.renderGanttItem,
          encode: { x: [0, 1], y: 2 },
          data: chartTasks
        }]
      }, true);
      this[chartName].resize();
    },
    renderGanttItem(params, api) {
      const categoryIndex = api.value(2);
      const start = api.coord([api.value(0), categoryIndex]);
      const end = api.coord([api.value(1), categoryIndex]);
      const height = Math.min(api.size([0, 1])[1] * 0.62, 26);
      const rectShape = echarts.graphic.clipRectByRect({
        x: start[0],
        y: start[1] - height / 2,
        width: Math.max(end[0] - start[0], 2),
        height
      }, {
        x: params.coordSys.x,
        y: params.coordSys.y,
        width: params.coordSys.width,
        height: params.coordSys.height
      });
      return rectShape ? { type: "rect", shape: rectShape, style: api.style() } : null;
    },
    getTaskType(task) {
      if (task.isInserted === "Y") return "INSERTED";
      if (task.isChanged === "Y") return "CHANGED";
      if (task.isFrozen === "Y") return "FROZEN";
      return "NORMAL";
    },
    buildTooltip(task) {
      const typeNames = { INSERTED: "插单", CHANGED: "变更", FROZEN: "冻结", NORMAL: "普通" };
      const lines = [
        "Lot / 订单编码：" + this.escapeHtml(task.orderCode || "-"),
        "工序编码：" + this.escapeHtml(task.processCode || "-"),
        "工序序号：" + this.escapeHtml(task.processSeq === null || task.processSeq === undefined ? "-" : task.processSeq),
        "设备编码：" + this.escapeHtml(task.equipmentCode || task.equipmentLabel || "-"),
        "开始时间：" + this.escapeHtml(task.plannedStartTime || "-"),
        "结束时间：" + this.escapeHtml(task.plannedEndTime || "-"),
        "加工时长：" + this.escapeHtml(task.displayDuration) + " 分钟",
        "任务类型：" + typeNames[task.taskType]
      ];
      if (task.taskType === "CHANGED" && task.originalStartTime) {
        const originalStart = this.toTime(task.originalStartTime);
        const newStart = this.toTime(task.plannedStartTime);
        lines.push("原开始时间：" + this.escapeHtml(task.originalStartTime));
        lines.push("新开始时间：" + this.escapeHtml(task.plannedStartTime || "-"));
        if (originalStart !== null && newStart !== null) {
          lines.push("后移分钟：" + this.escapeHtml(Math.round((newStart - originalStart) / 60000)));
        }
        if (task.originalEquipmentCode || task.originalEquipmentId) {
          lines.push("原设备：" + this.escapeHtml(task.originalEquipmentCode || task.originalEquipmentId));
          lines.push("新设备：" + this.escapeHtml(task.equipmentCode || task.equipmentLabel || "-"));
        }
      }
      return lines.join("<br/>");
    },
    escapeHtml(value) {
      return String(value).replace(/[&<>'"]/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "'": "&#39;",
        "\"": "&quot;"
      })[char]);
    },
    toTime(value) {
      if (!value) return null;
      const time = new Date(String(value).replace(/-/g, "/")).getTime();
      return Number.isNaN(time) ? null : time;
    },
    formatAxisTime(value) {
      const date = new Date(value);
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const hour = String(date.getHours()).padStart(2, "0");
      const minute = String(date.getMinutes()).padStart(2, "0");
      return `${month}-${day} ${hour}:${minute}`;
    },
    clearChartType(type) {
      this.setEquipments(type, []);
      this.$set(this.loading, type, false);
      this.$set(this.errors, type, null);
      this.disposeChart(type);
      this.$nextTick(() => {
        if (!this.sourcePlanId && !this.newPlanId) {
          this.cacheGeneration += 1;
          this.ganttCache = {};
          this.pendingRequests = {};
          this.activeTab = "source";
        }
      });
    },
    resetPreview() {
      this.sourceEquipments = [];
      this.candidateEquipments = [];
      this.cacheGeneration += 1;
      this.ganttCache = {};
      this.pendingRequests = {};
      this.loading = { source: false, candidate: false };
      this.errors = { source: null, candidate: null };
      this.activeTab = "source";
      this.disposeCharts();
    },
    disposeChart(type) {
      const chartName = type === "source" ? "sourceChart" : "candidateChart";
      if (this[chartName]) {
        this[chartName].dispose();
        this[chartName] = null;
      }
    },
    disposeCharts() {
      this.disposeChart("source");
      this.disposeChart("candidate");
    },
    resizeCharts() {
      if (this.sourceChart && !this.sourceChart.isDisposed()) this.sourceChart.resize();
      if (this.candidateChart && !this.candidateChart.isDisposed()) this.candidateChart.resize();
    },
    openFullGantt(planId) {
      if (!planId) return;
      this.$router.push({ path: "/aps/scheduleTask/gantt", query: { planId } });
    },
    openGanttCompare() {
      if (!this.newPlanId) return;
      this.$router.push({ path: "/aps/scheduleTask/ganttCompare", query: { planId: this.newPlanId } });
    }
  }
};
</script>

<style scoped>
.gantt-preview-card {
  margin-top: 12px;
  border-radius: 4px;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.preview-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.preview-actions .el-button {
  margin-left: 0;
}

.preview-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-bottom: 8px;
  color: #606266;
  font-size: 13px;
}

.preview-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-color {
  width: 18px;
  height: 8px;
  border-radius: 2px;
}

.legend-color.inserted { background: #ef4444; }
.legend-color.changed { background: #f59e0b; }
.legend-color.frozen { background: #22c55e; }
.legend-color.normal { background: #3b82f6; }

.preview-state-container {
  min-height: 320px;
}

.chart-scroll {
  max-height: 650px;
  overflow-y: auto;
  overflow-x: hidden;
}

.preview-chart {
  width: 100%;
  min-height: 320px;
}

@media (max-width: 768px) {
  .preview-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .preview-actions {
    justify-content: flex-start;
  }
}
</style>
