<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="el-icon-back" size="mini" @click="goBack">返回方案</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" icon="el-icon-refresh" size="mini" @click="loadCompare">刷新</el-button>
      </el-col>
    </el-row>

    <el-empty v-if="!planId" description="缺少重调度方案ID" />
    <template v-else>
      <el-descriptions v-if="sourcePlan && newPlan" :column="2" border size="small" class="mb12">
        <el-descriptions-item label="原方案">{{ sourcePlan.planId }} / {{ sourcePlan.planCode }} / {{ sourcePlan.planName }}</el-descriptions-item>
        <el-descriptions-item label="新方案">{{ newPlan.planId }} / {{ newPlan.planCode }} / {{ newPlan.planName }}</el-descriptions-item>
      </el-descriptions>

      <el-row :gutter="10" class="mb12">
        <el-col :span="1.5"><el-tag type="danger">插单任务</el-tag></el-col>
        <el-col :span="1.5"><el-tag type="warning">变更任务</el-tag></el-col>
        <el-col :span="1.5"><el-tag type="success">冻结任务</el-tag></el-col>
        <el-col :span="1.5"><el-tag>普通任务</el-tag></el-col>
      </el-row>

      <el-card shadow="never" class="mb12">
        <div slot="header">原方案甘特图</div>
        <el-empty v-if="!loading && sourceEquipments.length === 0" description="暂无原方案任务" />
        <div v-show="sourceEquipments.length > 0" ref="sourceChart" class="gantt-chart" :style="{ height: sourceChartHeight + 'px' }"></div>
      </el-card>

      <el-card shadow="never">
        <div slot="header">新方案甘特图</div>
        <el-empty v-if="!loading && newEquipments.length === 0" description="暂无新方案任务" />
        <div v-show="newEquipments.length > 0" ref="newChart" class="gantt-chart" :style="{ height: newChartHeight + 'px' }"></div>
      </el-card>
    </template>
  </div>
</template>

<script>
import * as echarts from "echarts";
import { getScheduleTaskGanttCompare } from "@/api/aps/scheduleTask";

export default {
  name: "ScheduleTaskGanttCompare",
  data() {
    return {
      loading: false,
      planId: null,
      sourcePlan: null,
      newPlan: null,
      sourceEquipments: [],
      newEquipments: [],
      sourceChart: null,
      newChart: null
    };
  },
  computed: {
    sourceChartHeight() {
      return Math.max(360, this.sourceEquipments.length * 58 + 110);
    },
    newChartHeight() {
      return Math.max(360, this.newEquipments.length * 58 + 110);
    }
  },
  mounted() {
    this.planId = this.$route.query.planId;
    this.loadCompare();
    window.addEventListener("resize", this.resizeCharts);
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.resizeCharts);
    if (this.sourceChart) {
      this.sourceChart.dispose();
    }
    if (this.newChart) {
      this.newChart.dispose();
    }
  },
  methods: {
    loadCompare() {
      if (!this.planId) {
        return;
      }
      this.loading = true;
      getScheduleTaskGanttCompare(this.planId).then(response => {
        const data = response.data || {};
        this.sourcePlan = data.sourcePlan;
        this.newPlan = data.newPlan;
        this.sourceEquipments = (data.sourceGantt && data.sourceGantt.equipments) || [];
        this.newEquipments = (data.newGantt && data.newGantt.equipments) || [];
        this.$nextTick(() => {
          this.renderChart("source");
          this.renderChart("new");
        });
      }).finally(() => {
        this.loading = false;
      });
    },
    renderChart(type) {
      const isNew = type === "new";
      const refName = isNew ? "newChart" : "sourceChart";
      const chartName = isNew ? "newChart" : "sourceChart";
      const equipments = isNew ? this.newEquipments : this.sourceEquipments;
      if (!this.$refs[refName]) {
        return;
      }
      if (!this[chartName]) {
        this[chartName] = echarts.init(this.$refs[refName]);
      }
      const categories = equipments.map(item => item.equipmentCode || String(item.equipmentId));
      const tasks = [];
      equipments.forEach((equipment, equipmentIndex) => {
        (equipment.tasks || []).forEach(task => {
          const start = this.toTime(task.plannedStartTime);
          const end = this.toTime(task.plannedEndTime);
          if (!start || !end) {
            return;
          }
          tasks.push({
            name: task.orderCode,
            value: [start, end, equipmentIndex, task, isNew ? "新方案" : "原方案"],
            itemStyle: {
              color: isNew ? this.colorByTaskType(task) : "#64748b"
            }
          });
        });
      });
      this[chartName].setOption({
        tooltip: { formatter: params => this.formatTooltip(params.value[3], params.value[4]) },
        grid: { left: 120, right: 40, top: 30, bottom: 48 },
        dataZoom: [
          { type: "inside", filterMode: "weakFilter" },
          { type: "slider", height: 20, bottom: 12, filterMode: "weakFilter" }
        ],
        xAxis: {
          type: "time",
          axisLabel: { formatter: value => this.formatAxisTime(value) }
        },
        yAxis: {
          type: "category",
          data: categories,
          inverse: true,
          axisLabel: { width: 110, overflow: "truncate" }
        },
        series: [{
          type: "custom",
          renderItem: this.renderGanttItem,
          encode: { x: [0, 1], y: 2 },
          data: tasks
        }]
      }, true);
      this.resizeCharts();
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
    formatTooltip(task, planType) {
      if (!task) {
        return "";
      }
      return [
        "方案类型：" + planType,
        "订单编码：" + (task.orderCode || ""),
        "产品编码：" + (task.productCode || ""),
        "工序序号：" + (task.processSeq || ""),
        "工序名称：" + (task.processName || ""),
        "设备编码：" + (task.equipmentCode || this.findEquipmentCode(task) || ""),
        "开始时间：" + (task.plannedStartTime || ""),
        "结束时间：" + (task.plannedEndTime || ""),
        "是否插单：" + (task.isInserted || "N"),
        "是否变更：" + (task.isChanged || "N"),
        "是否冻结：" + (task.isFrozen || "N"),
        "sourceTaskId：" + (task.sourceTaskId || "")
      ].join("<br/>");
    },
    findEquipmentCode(task) {
      const equipments = this.sourceEquipments.concat(this.newEquipments);
      const equipment = equipments.find(item => (item.tasks || []).some(inner => inner.taskId === task.taskId));
      return equipment ? equipment.equipmentCode : "";
    },
    colorByTaskType(task) {
      if (task.isInserted === "Y") {
        return "#ef4444";
      }
      if (task.isFrozen === "Y") {
        return "#22c55e";
      }
      if (task.isChanged === "Y") {
        return "#f59e0b";
      }
      return "#3b82f6";
    },
    toTime(value) {
      return value ? new Date(String(value).replace(/-/g, "/")).getTime() : null;
    },
    formatAxisTime(value) {
      const date = new Date(value);
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const hour = String(date.getHours()).padStart(2, "0");
      const minute = String(date.getMinutes()).padStart(2, "0");
      return `${month}-${day} ${hour}:${minute}`;
    },
    resizeCharts() {
      if (this.sourceChart) {
        this.sourceChart.resize();
      }
      if (this.newChart) {
        this.newChart.resize();
      }
    },
    goBack() {
      this.$router.push({ path: "/aps/schedulePlan" });
    }
  }
};
</script>

<style scoped>
.gantt-chart {
  width: 100%;
}
.mb12 {
  margin-bottom: 12px;
}
</style>
