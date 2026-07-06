<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="el-icon-back" size="mini" @click="goBack">返回任务</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" icon="el-icon-refresh" size="mini" @click="loadGantt">刷新</el-button>
      </el-col>
    </el-row>

    <el-empty v-if="!planId" description="缺少调度方案ID" />
    <el-empty v-else-if="!loading && equipments.length === 0" description="暂无甘特图任务数据" />
    <div v-show="planId && equipments.length > 0" ref="ganttChart" class="gantt-chart" :style="{ height: chartHeight + 'px' }"></div>
  </div>
</template>

<script>
import * as echarts from "echarts";
import { getScheduleTaskGantt } from "@/api/aps/scheduleTask";

export default {
  name: "ScheduleTaskGantt",
  data() {
    return {
      loading: false,
      planId: null,
      chart: null,
      equipments: []
    };
  },
  computed: {
    chartHeight() {
      return Math.max(400, this.equipments.length * 64 + 120);
    }
  },
  mounted() {
    this.planId = this.$route.query.planId;
    this.loadGantt();
    window.addEventListener("resize", this.resizeChart);
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.resizeChart);
    if (this.chart) {
      this.chart.dispose();
      this.chart = null;
    }
  },
  methods: {
    loadGantt() {
      if (!this.planId) {
        return;
      }
      this.loading = true;
      getScheduleTaskGantt(this.planId).then(response => {
        const data = response.data || {};
        this.equipments = data.equipments || [];
        this.$nextTick(() => {
          this.renderChart();
        });
      }).finally(() => {
        this.loading = false;
      });
    },
    renderChart() {
      if (!this.$refs.ganttChart) {
        return;
      }
      if (!this.chart) {
        this.chart = echarts.init(this.$refs.ganttChart);
      }
      const categories = this.equipments.map(item => item.equipmentCode || String(item.equipmentId));
      const tasks = [];
      this.equipments.forEach((equipment, equipmentIndex) => {
        (equipment.tasks || []).forEach(task => {
          const start = this.toTime(task.plannedStartTime);
          const end = this.toTime(task.plannedEndTime);
          if (!start || !end) {
            return;
          }
          tasks.push({
            name: task.orderCode,
            value: [start, end, equipmentIndex, task],
            itemStyle: {
              color: this.colorByOrder(task.orderCode)
            }
          });
        });
      });

      this.chart.setOption({
        tooltip: {
          formatter: params => this.formatTooltip(params.value[3])
        },
        grid: {
          left: 120,
          right: 40,
          top: 36,
          bottom: 50
        },
        dataZoom: [
          { type: "inside", filterMode: "weakFilter" },
          { type: "slider", height: 20, bottom: 14, filterMode: "weakFilter" }
        ],
        xAxis: {
          type: "time",
          axisLabel: {
            formatter: value => this.formatAxisTime(value)
          }
        },
        yAxis: {
          type: "category",
          data: categories,
          inverse: true,
          axisLabel: {
            width: 110,
            overflow: "truncate"
          }
        },
        series: [
          {
            type: "custom",
            renderItem: this.renderGanttItem,
            encode: { x: [0, 1], y: 2 },
            data: tasks
          }
        ]
      }, true);
      this.resizeChart();
    },
    renderGanttItem(params, api) {
      const categoryIndex = api.value(2);
      const start = api.coord([api.value(0), categoryIndex]);
      const end = api.coord([api.value(1), categoryIndex]);
      const height = Math.min(api.size([0, 1])[1] * 0.62, 28);
      const rectShape = echarts.graphic.clipRectByRect({
        x: start[0],
        y: start[1] - height / 2,
        width: end[0] - start[0],
        height
      }, {
        x: params.coordSys.x,
        y: params.coordSys.y,
        width: params.coordSys.width,
        height: params.coordSys.height
      });
      if (!rectShape) {
        return null;
      }
      return {
        type: "rect",
        shape: rectShape,
        style: api.style()
      };
    },
    formatTooltip(task) {
      if (!task) {
        return "";
      }
      return [
        "订单编码：" + (task.orderCode || ""),
        "产品编码：" + (task.productCode || ""),
        "工序序号：" + (task.processSeq || ""),
        "工序编码：" + (task.processCode || ""),
        "工序名称：" + (task.processName || ""),
        "设备编码：" + this.findEquipmentCode(task),
        "开始时间：" + (task.plannedStartTime || ""),
        "结束时间：" + (task.plannedEndTime || ""),
        "加工时长：" + (task.duration || "") + " 分钟",
        "是否插单：" + (task.isInserted || "N"),
        "是否冻结：" + (task.isFrozen || "N"),
        "是否变更：" + (task.isChanged || "N")
      ].join("<br/>");
    },
    findEquipmentCode(task) {
      const equipment = this.equipments.find(item =>
        (item.tasks || []).some(inner => inner.taskId === task.taskId)
      );
      return equipment ? equipment.equipmentCode : "";
    },
    toTime(value) {
      if (!value) {
        return null;
      }
      return new Date(String(value).replace(/-/g, "/")).getTime();
    },
    formatAxisTime(value) {
      const date = new Date(value);
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const hour = String(date.getHours()).padStart(2, "0");
      const minute = String(date.getMinutes()).padStart(2, "0");
      return `${month}-${day} ${hour}:${minute}`;
    },
    colorByOrder(orderCode) {
      const palette = ["#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4", "#84cc16", "#f97316"];
      const key = String(orderCode || "");
      let hash = 0;
      for (let i = 0; i < key.length; i++) {
        hash = (hash * 31 + key.charCodeAt(i)) >>> 0;
      }
      return palette[hash % palette.length];
    },
    resizeChart() {
      if (this.chart) {
        this.chart.resize();
      }
    },
    goBack() {
      this.$router.push({ path: "/aps/scheduleTask", query: { planId: this.planId } });
    }
  }
};
</script>

<style scoped>
.gantt-chart {
  width: 100%;
}
</style>
