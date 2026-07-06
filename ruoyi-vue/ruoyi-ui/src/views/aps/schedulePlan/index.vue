<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="方案编码" prop="planCode">
        <el-input v-model="queryParams.planCode" placeholder="请输入方案编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="方案名称" prop="planName">
        <el-input v-model="queryParams.planName" placeholder="请输入方案名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="方案类型" prop="planType">
        <el-input v-model="queryParams.planType" placeholder="INITIAL / RESCHEDULE" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="方案状态" prop="planStatus">
        <el-input v-model="queryParams.planStatus" placeholder="ACTIVE / HISTORY" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="有效标识" prop="activeFlag">
        <el-input v-model="queryParams.activeFlag" placeholder="Y / N" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aps:schedulePlan:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-video-play" size="mini" @click="handleGenerateInitial" v-hasPermi="['aps:schedulePlan:add']">生成初始调度</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aps:schedulePlan:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aps:schedulePlan:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aps:schedulePlan:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="schedulePlanList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="方案ID" align="center" prop="planId" width="90" />
      <el-table-column label="方案编码" align="center" prop="planCode" :show-overflow-tooltip="true" />
      <el-table-column label="方案名称" align="center" prop="planName" :show-overflow-tooltip="true" />
      <el-table-column label="方案类型" align="center" prop="planType" width="110" />
      <el-table-column label="方案状态" align="center" prop="planStatus" width="110" />
      <el-table-column label="算法名称" align="center" prop="algorithmName" :show-overflow-tooltip="true" />
      <el-table-column label="策略类型" align="center" prop="strategyType" :show-overflow-tooltip="true" />
      <el-table-column label="开始时间" align="center" prop="scheduleStartTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.scheduleStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="scheduleEndTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.scheduleEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="有效" align="center" prop="activeFlag" width="80" />
      <el-table-column label="KPI" align="center" prop="kpiJson" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="300">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-tickets" @click="handleViewTasks(scope.row)" v-hasPermi="['aps:scheduleTask:list']">查看任务</el-button>
          <el-button size="mini" type="text" icon="el-icon-data-analysis" @click="handleViewGantt(scope.row)" v-hasPermi="['aps:scheduleTask:list']">查看甘特图</el-button>
          <el-button v-if="scope.row.planType === 'RESCHEDULE'" size="mini" type="text" icon="el-icon-s-data" @click="handleCompare(scope.row)" v-hasPermi="['aps:schedulePlan:list']">方案对比</el-button>
          <el-button v-if="scope.row.planType === 'RESCHEDULE'" size="mini" type="text" icon="el-icon-data-line" @click="handleGanttCompare(scope.row)" v-hasPermi="['aps:scheduleTask:list']">甘特图对比</el-button>
          <el-button v-if="canConfirmOrReject(scope.row)" size="mini" type="text" icon="el-icon-check" @click="handleConfirmPlan(scope.row)" v-hasPermi="['aps:schedulePlan:edit']">采用方案</el-button>
          <el-button v-if="canConfirmOrReject(scope.row)" size="mini" type="text" icon="el-icon-close" @click="handleRejectPlan(scope.row)" v-hasPermi="['aps:schedulePlan:edit']">拒绝方案</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aps:schedulePlan:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aps:schedulePlan:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="方案 KPI 对比" :visible.sync="compareOpen" width="860px" append-to-body>
      <template v-if="compareDetail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="原方案">{{ compareDetail.sourcePlanId }} / {{ compareDetail.sourcePlanCode }}</el-descriptions-item>
          <el-descriptions-item label="新方案">{{ compareDetail.newPlanId }} / {{ compareDetail.newPlanCode }}</el-descriptions-item>
          <el-descriptions-item label="插单事件">{{ compareDetail.eventId }}</el-descriptions-item>
          <el-descriptions-item label="插单订单">{{ compareDetail.insertOrderCode }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions class="mt12" :column="3" border size="small" title="任务变化">
          <el-descriptions-item label="原任务数">{{ compareDetail.summary.originalTaskCount }}</el-descriptions-item>
          <el-descriptions-item label="新任务数">{{ compareDetail.summary.newTaskCount }}</el-descriptions-item>
          <el-descriptions-item label="变更任务数">{{ compareDetail.summary.changedTaskCount }}</el-descriptions-item>
          <el-descriptions-item label="变更比例">{{ compareDetail.summary.changedTaskRatio }}</el-descriptions-item>
          <el-descriptions-item label="插单任务数">{{ compareDetail.summary.insertedTaskCount }}</el-descriptions-item>
          <el-descriptions-item label="冻结任务数">{{ compareDetail.summary.frozenTaskCount }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions class="mt12" :column="3" border size="small" title="延期对比">
          <el-descriptions-item label="原延期订单数">{{ compareDetail.delayCompare.originalDelayOrderCount }}</el-descriptions-item>
          <el-descriptions-item label="新延期订单数">{{ compareDetail.delayCompare.newDelayOrderCount }}</el-descriptions-item>
          <el-descriptions-item label="变化">{{ compareDetail.delayCompare.delayOrderCountDiff }}</el-descriptions-item>
          <el-descriptions-item label="原总延期分钟">{{ compareDetail.delayCompare.originalTotalDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="新总延期分钟">{{ compareDetail.delayCompare.newTotalDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="变化">{{ compareDetail.delayCompare.totalDelayMinutesDiff }}</el-descriptions-item>
          <el-descriptions-item label="原最大延期分钟">{{ compareDetail.delayCompare.originalMaxDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="新最大延期分钟">{{ compareDetail.delayCompare.newMaxDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="变化">{{ compareDetail.delayCompare.maxDelayMinutesDiff }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions class="mt12" :column="2" border size="small" title="完工时间">
          <el-descriptions-item label="原 makespan">{{ compareDetail.makespanCompare.originalMakespan }}</el-descriptions-item>
          <el-descriptions-item label="新 makespan">{{ compareDetail.makespanCompare.newMakespan }}</el-descriptions-item>
          <el-descriptions-item label="makespan 变化分钟">{{ compareDetail.makespanCompare.makespanDiffMinutes }}</el-descriptions-item>
          <el-descriptions-item label="插单完工时间">{{ compareDetail.insertOrderCompare.insertOrderFinishTime }}</el-descriptions-item>
          <el-descriptions-item label="插单延期分钟">{{ compareDetail.insertOrderCompare.insertOrderDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="结论">{{ compareDetail.conclusion }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body>
      <el-form ref="form" :model="form" label-width="100px">
        <el-form-item label="方案编码" prop="planCode">
          <el-input v-model="form.planCode" placeholder="请输入方案编码" />
        </el-form-item>
        <el-form-item label="方案名称" prop="planName">
          <el-input v-model="form.planName" placeholder="请输入方案名称" />
        </el-form-item>
        <el-form-item label="方案类型" prop="planType">
          <el-input v-model="form.planType" placeholder="INITIAL / RESCHEDULE" />
        </el-form-item>
        <el-form-item label="方案状态" prop="planStatus">
          <el-input v-model="form.planStatus" placeholder="PENDING / ACTIVE / HISTORY" />
        </el-form-item>
        <el-form-item label="算法名称" prop="algorithmName">
          <el-input v-model="form.algorithmName" placeholder="请输入算法名称" />
        </el-form-item>
        <el-form-item label="策略类型" prop="strategyType">
          <el-input v-model="form.strategyType" placeholder="请输入策略类型" />
        </el-form-item>
        <el-form-item label="开始时间" prop="scheduleStartTime">
          <el-date-picker clearable v-model="form.scheduleStartTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择开始时间" />
        </el-form-item>
        <el-form-item label="结束时间" prop="scheduleEndTime">
          <el-date-picker clearable v-model="form.scheduleEndTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择结束时间" />
        </el-form-item>
        <el-form-item label="KPI" prop="kpiJson">
          <el-input v-model="form.kpiJson" type="textarea" placeholder="请输入KPI JSON" />
        </el-form-item>
        <el-form-item label="有效标识" prop="activeFlag">
          <el-input v-model="form.activeFlag" placeholder="Y / N" />
        </el-form-item>
        <el-form-item label="删除标识" prop="delFlag">
          <el-input v-model="form.delFlag" placeholder="0 / 1" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSchedulePlan, getSchedulePlan, delSchedulePlan, addSchedulePlan, updateSchedulePlan, generateInitialSchedule, compareSchedulePlan, confirmSchedulePlan, rejectSchedulePlan } from "@/api/aps/schedulePlan";

export default {
  name: "SchedulePlan",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      schedulePlanList: [],
      title: "",
      open: false,
      compareOpen: false,
      compareDetail: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        planCode: null,
        planName: null,
        planType: null,
        planStatus: null,
        activeFlag: null
      },
      form: {}
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listSchedulePlan(this.queryParams).then(response => {
        this.schedulePlanList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        planId: null,
        planCode: null,
        planName: null,
        planType: null,
        planStatus: null,
        sourcePlanId: null,
        eventId: null,
        algorithmName: null,
        strategyType: null,
        scheduleStartTime: null,
        scheduleEndTime: null,
        kpiJson: null,
        aiExplanation: null,
        activeFlag: null,
        delFlag: "0",
        remark: null
      };
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.planId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "新增调度方案";
    },
    handleGenerateInitial() {
      this.$modal.confirm("确认基于当前普通 NEW 订单生成初始调度方案？").then(() => {
        return generateInitialSchedule();
      }).then(response => {
        const taskCount = response.data && response.data.taskCount ? response.data.taskCount : 0;
        this.$modal.msgSuccess("初始调度生成成功，任务数：" + taskCount);
        this.getList();
      }).catch(() => {});
    },
    handleUpdate(row) {
      this.reset();
      const planId = row.planId || this.ids;
      getSchedulePlan(planId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改调度方案";
      });
    },
    handleViewTasks(row) {
      this.$router.push({ path: "/aps/scheduleTask", query: { planId: row.planId } });
    },
    handleViewGantt(row) {
      this.$router.push({ path: "/aps/scheduleTask/gantt", query: { planId: row.planId } });
    },
    handleCompare(row) {
      compareSchedulePlan(row.planId).then(response => {
        this.compareDetail = response.data;
        this.compareOpen = true;
      });
    },
    handleGanttCompare(row) {
      this.$router.push({ path: "/aps/scheduleTask/ganttCompare", query: { planId: row.planId } });
    },
    canConfirmOrReject(row) {
      return row.planType === "RESCHEDULE" && row.planStatus === "PENDING";
    },
    handleConfirmPlan(row) {
      this.$modal.confirm("确定采用该重调度方案吗？采用后原方案将转为历史方案。").then(() => {
        return confirmSchedulePlan(row.planId);
      }).then(() => {
        this.$modal.msgSuccess("方案已确认采用");
        this.getList();
      }).catch(() => {});
    },
    handleRejectPlan(row) {
      this.$modal.confirm("确定拒绝该重调度方案吗？拒绝后系统将恢复原初始方案为当前有效方案。").then(() => {
        return rejectSchedulePlan(row.planId);
      }).then(() => {
        this.$modal.msgSuccess("方案已拒绝");
        this.getList();
      }).catch(() => {});
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.planId != null) {
            updateSchedulePlan(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addSchedulePlan(this.form).then(() => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const planIds = row.planId || this.ids;
      this.$modal.confirm('是否确认删除调度方案编号为"' + planIds + '"的数据项？').then(function() {
        return delSchedulePlan(planIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download("aps/schedulePlan/export", {
        ...this.queryParams
      }, `schedulePlan_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>

<style scoped>
.mt12 {
  margin-top: 12px;
}
</style>
